package com.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.agents.config.BraveConfirmationHandler
import com.agents.config.ConfirmationHandler
import com.agents.config.SafeConfirmationHandler
import com.agents.mcp.McpClient
import com.agents.mcp.McpToolDiscovery
import com.agents.tools.*

suspend fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: kotlin-ai-agent-koog <project-path> <task> [--brave]")
        println("Example: kotlin-ai-agent-koog /path/to/project \"Add a function to calculate fibonacci numbers\"")
        println("\nOptions:")
        println("  --brave    Enable brave mode (auto-approve all operations)")
        return
    }

    val projectPath = args[0]
    val task = args[1]
    val braveMode = args.getOrNull(2) == "--brave"

    val apiKey = System.getenv("OPENAI_API_KEY") ?: run {
        // Fallback to .env file
        val envFile = java.io.File(".env")
        if (envFile.exists()) {
            envFile.readLines()
                .firstOrNull { it.startsWith("OPENAI_API_KEY=") }
                ?.substringAfter("OPENAI_API_KEY=")
                ?: throw IllegalStateException("OPENAI_API_KEY not found in .env file")
        } else {
            throw IllegalStateException("OPENAI_API_KEY environment variable not set and .env file not found")
        }
    }

    // Configure confirmation handler based on mode
    val confirmationHandler: ConfirmationHandler = if (braveMode) {
        println("[Brave Mode] Auto-approving all operations")
        BraveConfirmationHandler()
    } else {
        SafeConfirmationHandler()
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

    // Combine all tools
    val allTools = fileSystemTools + mcpTools

    // Create Koog AI Agent
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

            ## Your Approach

            1. **Explore**: Start by exploring the project structure with list__directory
            2. **Understand**: Read relevant files to understand the current implementation
            3. **Plan**: Think through the changes needed before making them
            4. **Implement**: Use create__file for new files, edit__file for existing files
            5. **Build/Test**: Use execute__shell_command to run builds, tests, or other tools
            6. **Verify**: After changes, consider reading files back to verify
            7. **Commit**: Use git tools when appropriate to track changes
            8. **Report**: Provide a clear summary of what you changed and why

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

    val result = agent.run(input)

    println("\n" + "=".repeat(80))
    println("FINAL RESULT:")
    println(result)
}
