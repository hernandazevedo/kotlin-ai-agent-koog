# Sub-Agent Implementation

This document describes the sub-agent architecture implemented in this project, based on [JetBrains Koog blog post Part 4: Building AI Agents in Kotlin - Delegation and Sub-Agents](https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-4-delegation-and-sub-agents/).

## Overview

Sub-agents are specialized AI agents that can be delegated tasks by a main agent. This architecture provides several benefits:

- **Reduced Context**: Each sub-agent works with a focused toolset and system prompt
- **Cost Optimization**: Sub-agents can use cheaper/smaller LLM models for specialized tasks
- **Modularity**: Different agents can be experts in different domains
- **Team-like Structure**: Mimics how software teams delegate work to specialists

## Implementation

### CodeSearchAgent

The `CodeSearchAgent` is a specialized sub-agent for searching and finding code in the codebase.

**Location**: `src/jvmMain/kotlin/com/agents/subagents/CodeSearchAgent.kt`

**Purpose**: Efficiently search through codebases to find implementations, patterns, or understand project structure.

**Tools Available**:
- `list__directory`: Lists files and directories
- `read__file`: Reads file contents

**Model**: Uses `GPT4oMini` for cost-effective search operations

### Integration Pattern

The sub-agent is integrated into the main agent through a wrapper tool:

```kotlin
// 1. Create the sub-agent with focused capabilities
fun createCodeSearchAgent(apiKey: String): AIAgent<String, String> {
    return AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        systemPrompt = "...",  // Focused search instructions
        llmModel = OpenAIModels.Chat.GPT4oMini,
        toolRegistry = ToolRegistry {
            // Limited toolset for search operations
        }
    )
}

// 2. Wrap it as a tool that the main agent can call
class CodeSearchAgentTool(
    private val agent: AIAgent<String, String>
) : SimpleTool<CodeSearchAgentTool.Args>(...) {
    override suspend fun execute(args: Args): String {
        return agent.run(args.query)
    }
}

// 3. Add it to the main agent's toolset
val codeSearchAgentTool = createCodeSearchAgentTool(apiKey)
val allTools = fileSystemTools + mcpTools + codeSearchAgentTool
```

## Usage

When the main agent needs to find code, it can delegate to the sub-agent:

```kotlin
// Main agent receives task: "Add error handling to the file writing logic"

// Main agent delegates search:
// Tool: __find_in_codebase_agent__
// Input: "Find the implementation of the file writing logic"

// Sub-agent searches and returns:
// FOUND: File write implementation
// LOCATION: src/commonMain/kotlin/com/agents/tools/FileSystemTools.kt:108
//
// RELEVANT CODE:
// class CreateFileTool(
//     private val fileSystem: FileSystemProvider,
//     private val confirmationHandler: ConfirmationHandler
// ) : SimpleTool<CreateFileTool.Args>(...)
//
// CONTEXT: This is the main file creation tool with confirmation handling

// Main agent can now read and modify the specific file
```

## Benefits Observed

Based on the JetBrains blog post, implementing sub-agents can provide:

- **~10% cost reduction** by limiting context growth
- **Improved accuracy** by using specialized prompts
- **Better organization** through clear delegation patterns

## Future Enhancements

Potential sub-agents to add:

1. **TestAgent**: Specialized in running tests and interpreting test failures
2. **RefactorAgent**: Expert in code refactoring patterns
3. **DocumentationAgent**: Focuses on generating and updating documentation
4. **GitAgent**: Handles all git operations and commit messages

## Architecture Notes

- Sub-agents are stateless and created per-request
- Each sub-agent has a focused system prompt and limited toolset
- Main agent decides when to delegate to sub-agents
- Sub-agents return structured results for easy parsing
