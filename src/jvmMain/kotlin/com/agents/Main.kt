package com.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.feature.*
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.agents.config.BraveConfirmationHandler
import com.agents.config.ConfirmationHandler
import com.agents.config.InteractiveConfirmationHandler
import com.agents.config.SafeConfirmationHandler
import com.agents.mcp.McpClient
import com.agents.mcp.McpToolDiscovery
import com.agents.subagents.createCodeSearchAgentTool
import com.agents.tools.*
import java.util.UUID

/**
 * Helper function to read environment variables with fallback to .env file
 * Following the pattern from JetBrains Koog observability best practices
 */
fun getEnvOrDotEnv(key: String): String? {
    return System.getenv(key) ?: run {
        val envFile = java.io.File(".env")
        if (envFile.exists()) {
            envFile.readLines()
                .firstOrNull { it.startsWith("$key=") }
                ?.substringAfter("$key=")
        } else {
            null
        }
    }
}

suspend fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: kotlin-ai-agent-koog <project-path> <task> [--brave] [--interactive] [--user <userId>]")
        println("Example: kotlin-ai-agent-koog /path/to/project \"Add a function to calculate fibonacci numbers\"")
        println("\nOptions:")
        println("  --brave         Enable brave mode (auto-approve all operations)")
        println("  --interactive   Enable interactive mode (show diff and prompt for approval)")
        println("  --user <userId> Set user ID for Langfuse tracking (can also use LANGFUSE_USER_ID env var)")
        return
    }

    val projectPath = args[0]
    val task = args[1]

    // Parse optional flags
    var braveMode = false
    var interactiveMode = false
    var userIdFromArgs: String? = null

    var i = 2
    while (i < args.size) {
        when (args[i]) {
            "--brave" -> braveMode = true
            "--interactive" -> interactiveMode = true
            "--user" -> {
                if (i + 1 < args.size) {
                    userIdFromArgs = args[i + 1]
                    i++ // Skip next arg since it's the user ID value
                }
            }
        }
        i++
    }

    // Read OpenAI API key
    val apiKey = getEnvOrDotEnv("OPENAI_API_KEY")
        ?: throw IllegalStateException("OPENAI_API_KEY environment variable not set and not found in .env file")

    // Read Langfuse configuration for observability
    val langfusePublicKey = getEnvOrDotEnv("LANGFUSE_PUBLIC_KEY")
    val langfuseSecretKey = getEnvOrDotEnv("LANGFUSE_SECRET_KEY")
    val langfuseBaseUrl = getEnvOrDotEnv("LANGFUSE_BASE_URL")

    // Determine user ID: prioritize CLI arg, fall back to env var
    val userId = userIdFromArgs ?: getEnvOrDotEnv("LANGFUSE_USER_ID")

    // Generate a unique session ID for this agent run
    val sessionId = "agent-run-${UUID.randomUUID().toString().take(8)}"

    // Check if Langfuse is configured
    val langfuseConfigured = langfusePublicKey != null && langfuseSecretKey != null && langfuseBaseUrl != null
    if (langfuseConfigured) {
        val userInfo = userId?.let { " - User ID: $it" } ?: ""
        println("[Observability] Langfuse tracking enabled on host $langfuseBaseUrl - Session ID: $sessionId$userInfo")

        // Configure OpenTelemetry to send traces to Langfuse via OTLP
        // Langfuse accepts OTLP traces at /api/public/otel endpoint
        val otlpEndpoint = "${langfuseBaseUrl!!.trimEnd('/')}/api/public/otel"

        // Create Basic Auth header: base64(publicKey:secretKey)
        val authString = "$langfusePublicKey:$langfuseSecretKey"
        val authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(authString.toByteArray())

        // Set OpenTelemetry environment variables for OTLP exporter
        System.setProperty("OTEL_EXPORTER_OTLP_ENDPOINT", otlpEndpoint)
        System.setProperty("OTEL_EXPORTER_OTLP_HEADERS", "Authorization=$authHeader")
        System.setProperty("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf")

        println("[Observability] OTLP endpoint: $otlpEndpoint")
    } else {
        println("[Observability] Langfuse not configured - skipping telemetry")
    }

    // Configure confirmation handler based on mode
    val confirmationHandler: ConfirmationHandler = when {
        braveMode -> {
            println("[Brave Mode] Auto-approving all operations")
            BraveConfirmationHandler()
        }
        interactiveMode -> {
            println("[Interactive Mode] Will prompt for approval with diff preview")
            InteractiveConfirmationHandler(useColors = true)
        }
        else -> {
            println("[Safe Mode] Using default safe confirmation handler")
            SafeConfirmationHandler()
        }
    }

    // Initialize file system tools with confirmation handler
    val fileSystemTools = listOf(
        ListDirectoryTool(FileSystem.readOnly),
        ReadFileTool(FileSystem.readOnly),
        CreateFileTool(FileSystem.readWrite, confirmationHandler),
        EditFileTool(FileSystem.readWrite, confirmationHandler),
        ExecuteShellCommandTool(confirmationHandler)
    )

    // Initialize MCP client for Git operations
    val mcpClient = McpClient(
        serverUrl = System.getenv("MCP_SERVER_URL") ?: "http://localhost:8080/mcp",
        verbose = false
    )

    // Auto-discover Git tools from MCP server
    val mcpDiscovery = McpToolDiscovery(mcpClient, verbose = true)
    val mcpTools = mcpDiscovery.discoverTools()

    // Create code search sub-agent tool
    val codeSearchAgentTool = createCodeSearchAgentTool(apiKey)

    // Combine all tools (including sub-agent tool)
    val allTools = fileSystemTools + mcpTools + codeSearchAgentTool

    // Create Koog AI Agent with observability
    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        systemPrompt = """
            You are a highly skilled programmer tasked with updating the provided codebase according to the given task.

            ## Available Tools

            ### File System Tools
            - list__directory: Lists all files and directories in a path
            - read__file: Reads the contents of a file
            - create__file: Creates a new file with specified content (for NEW files only)
            - edit__file: Modifies an existing file by replacing its content (for EXISTING files only)
            - execute__shell_command: Executes shell commands with timeout support (build tools, scripts, etc.)

            ### Git Tools (if MCP server is running)
            - git_status: Show the working tree status
            - git_diff: Show changes between commits, commit and working tree, etc
            - git_commit: Record changes to the repository
            - git_log: Show commit logs
            - git_branch: List, create, or delete branches
            - git_checkout: Switch branches or restore working tree files
            - git_add: Add file contents to the staging area
            - git_push: Update remote refs along with associated objects

            ### Sub-Agent Tools
            - __find_in_codebase_agent__: Specialized agent for searching and finding code in the codebase
              Use this when you need to find specific implementations, patterns, or understand project structure

            ## Your Approach

            1. **Search**: When you need to find code, use __find_in_codebase_agent__ to delegate search tasks
            2. **Explore**: Use list__directory to explore project structure when you know what you're looking for
            3. **Understand**: Read relevant files to understand the current implementation
            4. **Plan**: Think through the changes needed before making them
            5. **Implement**: Use create__file for new files, edit__file for existing files
            6. **Build/Test**: Use execute__shell_command to run builds, tests, or other tools
            7. **Verify**: After changes, consider reading files back to verify
            8. **Commit**: Use git tools when appropriate to track changes
            9. **Report**: Provide a clear summary of what you changed and why

            ## Important Guidelines

            - Be strategic about which files to read - avoid reading unnecessary files
            - Always validate paths before operations
            - Use execute__shell_command for running builds, tests, or any system commands
            - Set appropriate timeouts for long-running commands (default: 30s)
            - Provide helpful error messages if operations fail
            - When errors occur, adjust your approach based on the error message
            - Read files before editing them to understand current state
            - Use appropriate tools: create__file for new files, edit__file for existing ones

            ## Definition of Done

            Your task is complete when:
            1. All required code changes have been successfully implemented
            2. Files have been created or modified as needed
            3. Changes align with the task requirements
            4. You have verified the changes were applied correctly
            5. You have provided a clear summary of what was changed and why

            If you encounter errors or cannot complete the task, explain what went wrong and what would be needed to resolve it.
        """.trimIndent(),
        llmModel = OpenAIModels.Chat.GPT4o,
        toolRegistry = ToolRegistry {
            allTools.forEach { tool(it) }
        },
    ) {
        // Install OpenTelemetry with Langfuse for observability
        // Following JetBrains Koog best practices from blog article Part 3
        // Traces are sent to Langfuse via OTLP (configured via system properties above)
        if (langfuseConfigured) {
            install(OpenTelemetry) {
                setVerbose(true) // Send full strings instead of HIDDEN placeholders
                addLangfuseExporter(
                    langfuseUrl = langfuseBaseUrl,
                    langfusePublicKey = langfusePublicKey,
                    langfuseSecretKey = langfuseSecretKey,
                    traceAttributes = buildList {
                        add(CustomAttribute("langfuse.session.id", sessionId))
                        userId?.let { add(CustomAttribute("langfuse.user.id", it)) }
                    }
                )
            }
        }

        // Handle events for console logging
        handleEvents {
            onToolCallStarting { ctx ->
                println("Tool '${ctx.toolName}' called with args: ${ctx.toolArgs.toString().take(100)}")
            }
        }
    }

    val input = "Project absolute path: $projectPath\n\n## Task\n$task"

    println("Starting AI Agent...")
    println("Project: $projectPath")
    println("Task: $task")
    println("=" .repeat(80))

    // Use try-finally to ensure agent is properly closed
    val result = try {
        agent.run(input)
    } finally {
        // Close the agent to flush telemetry traces
        agent.close()
        if (langfuseConfigured) {
            println("[Observability] Agent closed, traces flushed to Langfuse")
        }
    }

    println("\n" + "=".repeat(80))
    println("FINAL RESULT:")
    println(result)
}
