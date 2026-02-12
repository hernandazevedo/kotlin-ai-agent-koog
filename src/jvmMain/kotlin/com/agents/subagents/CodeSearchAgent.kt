package com.agents.subagents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.agents.FileSystem
import com.agents.tools.ListDirectoryTool
import com.agents.tools.ReadFileTool
import kotlinx.serialization.Serializable

/**
 * CodeSearchAgent - A specialized sub-agent for finding code in the codebase
 *
 * This agent is designed to be called by the main agent when it needs to search
 * through the codebase to find specific implementations, patterns, or files.
 *
 * Based on JetBrains Koog blog post Part 4: Building AI Agents in Kotlin - Delegation and Sub-Agents
 * https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-4-delegation-and-sub-agents/
 *
 * Key benefits:
 * - Reduces context size by focusing only on search operations
 * - Can use a smaller/cheaper model for focused search tasks
 * - Provides structured search results back to the parent agent
 */
fun createCodeSearchAgent(apiKey: String): AIAgent<String, String> {
    // Create a focused toolset for code search operations
    val searchTools = listOf(
        ListDirectoryTool(FileSystem.readOnly),
        ReadFileTool(FileSystem.readOnly)
    )

    return AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        systemPrompt = """
            You are a specialized code search agent. Your sole purpose is to find code in a codebase.

            ## Your Role

            When the main agent needs to find:
            - Specific function or class implementations
            - Files containing certain patterns
            - Project structure understanding
            - Code examples or usage patterns

            They will delegate the search to you.

            ## Available Tools

            - list__directory: List files and directories in a path
            - read__file: Read file contents

            ## Your Process

            1. **Start with structure**: Use list__directory to understand the project layout
            2. **Navigate intelligently**: Look in likely places (src/, main/, lib/, etc.)
            3. **Read strategically**: Only read files that are likely to contain what you're looking for
            4. **Return focused results**: Include:
               - File paths where the code was found
               - Relevant code snippets (not entire files)
               - Brief explanation of what you found
               - Line numbers or locations when possible

            ## Important Guidelines

            - Be efficient - minimize the number of files you read
            - Focus on finding what was asked for, not exploring everything
            - If you can't find something after reasonable searching, say so
            - Return structured, actionable information
            - Include enough context that the main agent can use your findings

            ## Output Format

            When you find code, respond with:
            ```
            FOUND: [what you found]
            LOCATION: [file path]:[line/section]

            RELEVANT CODE:
            [code snippet]

            CONTEXT: [brief explanation]
            ```

            When you don't find code, respond with:
            ```
            NOT FOUND: [what you searched for]
            SEARCHED: [list of locations you checked]
            SUGGESTION: [hints for the main agent]
            ```
        """.trimIndent(),
        llmModel = OpenAIModels.Chat.GPT4oMini, // Use cheaper model for focused search tasks
        toolRegistry = ToolRegistry {
            searchTools.forEach { tool(it) }
        }
    )
}

/**
 * Wrapper tool that delegates code search requests to the CodeSearchAgent
 */
class CodeSearchAgentTool(
    private val agent: AIAgent<String, String>
) : SimpleTool<CodeSearchAgentTool.Args>(
    name = "__find_in_codebase_agent__",
    description = """
        Specialized agent for searching and finding code in the codebase.

        Use this agent when you need to:
        - Find where a specific function or class is implemented
        - Search for files containing certain patterns or keywords
        - Understand the project structure before making changes
        - Locate examples of how certain APIs or patterns are used
        - Find related code across multiple files

        This agent is optimized for code search and will explore the codebase
        systematically to find what you're looking for.

        Input should be a clear description of what code you're looking for,
        including any context about why you need it.

        Examples:
        - "Find the implementation of the file writing logic"
        - "Search for all classes that use ConfirmationHandler"
        - "Locate test files related to file system operations"
        - "Find where OpenAI API integration is configured"
    """.trimIndent(),
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(
        val query: String
    )

    override suspend fun execute(args: Args): String {
        println("[SubAgent] CodeSearchAgent invoked with query: ${args.query.take(100)}${if (args.query.length > 100) "..." else ""}")
        val result = agent.run(args.query)
        println("[SubAgent] CodeSearchAgent completed search")
        return result
    }
}
