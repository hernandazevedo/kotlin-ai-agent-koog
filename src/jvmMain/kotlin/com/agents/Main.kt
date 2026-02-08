package com.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.agents.mcp.McpClient
import com.agents.mcp.McpToolDiscovery
import com.agents.tools.*
import kotlinx.coroutines.runBlocking

suspend fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: kotlin-ai-agent-koog <project-path> <task>")
        println("Example: kotlin-ai-agent-koog /path/to/project \"Add a function to calculate fibonacci numbers\"")
        return
    }

    val projectPath = args[0]
    val task = args[1]

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

    // Initialize file system tools
    val fileSystemTools = listOf(
        ListDirectoryTool(FileSystem.readOnly),
        ReadFileTool(FileSystem.readOnly),
        CreateFileTool(FileSystem.readWrite),
        EditFileTool(FileSystem.readWrite)
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

            You have access to the following file system tools:
            - list_directory: Lists all files and directories in a path
            - read_file: Reads the contents of a file
            - create_file: Creates a new file with specified content
            - edit_file: Modifies an existing file by replacing its content

            You also have access to Git tools (if MCP server is running):
            - git_status: Show the working tree status
            - git_diff: Show changes between commits, commit and working tree, etc
            - git_commit: Record changes to the repository
            - git_log: Show commit logs
            - git_branch: List, create, or delete branches
            - git_checkout: Switch branches or restore working tree files
            - git_add: Add file contents to the staging area

            Your approach should be:
            1. First, explore the project structure using list_directory
            2. Read relevant files to understand the current implementation
            3. Create new files using create_file when needed
            4. Modify existing files using edit_file
            5. Use git tools to commit changes when appropriate
            6. Provide a clear summary of what you changed and why

            Be strategic about which files to read - avoid reading unnecessary files to conserve context.
            Always provide informative error messages if something goes wrong.
        """.trimIndent(),
        llmModel = OpenAIModels.Chat.GPT4o,
        toolRegistry = ToolRegistry {
            allTools.forEach { tool(it) }
        }
    )

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
