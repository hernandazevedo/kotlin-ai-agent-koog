package com.agents.subagents

import ai.koog.agents.core.tools.Tool

/**
 * Creates a tool wrapper for the CodeSearchAgent that can be used by the main agent
 *
 * Based on JetBrains Koog blog post Part 4: Building AI Agents in Kotlin - Delegation and Sub-Agents
 * https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-4-delegation-and-sub-agents/
 *
 * This wraps the sub-agent as a SimpleTool that can be called by parent agents.
 */
fun createCodeSearchAgentTool(apiKey: String): Tool<*, *> {
    val codeSearchAgent = createCodeSearchAgent(apiKey)
    return CodeSearchAgentTool(codeSearchAgent)
}
