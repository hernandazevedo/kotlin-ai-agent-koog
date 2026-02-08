package com.agents.mcp

import ai.koog.agents.core.tools.SimpleTool

/**
 * Discovers and returns MCP tools as Koog SimpleTool instances
 */
class McpToolDiscovery(
    private val mcpClient: McpClient,
    private val verbose: Boolean = false
) {
    /**
     * Initialize connection, discover tools from MCP server, and return them as Koog tools
     */
    fun discoverTools(): List<SimpleTool<*>> {
        if (verbose) {
            println("[MCP Discovery] Checking if MCP server is available...")
        }

        if (!mcpClient.isServerAvailable()) {
            println("[MCP Discovery] Warning: MCP server is not available. Skipping tool discovery.")
            return emptyList()
        }

        return try {
            if (verbose) {
                println("[MCP Discovery] Initializing connection...")
            }

            // Initialize the MCP connection
            val initResult = mcpClient.initialize()
            if (verbose) {
                println("[MCP Discovery] Connected to ${initResult.serverInfo.name} v${initResult.serverInfo.version}")
                println("[MCP Discovery] Protocol version: ${initResult.protocolVersion}")
            }

            // List all available tools from the MCP server
            if (verbose) {
                println("[MCP Discovery] Discovering tools...")
            }

            val mcpTools = mcpClient.listTools()
            if (verbose) {
                println("[MCP Discovery] Found ${mcpTools.size} tools")
            }

            // Create adapters for each tool
            val koogTools = mcpTools.map { mcpTool ->
                val adapter = McpToolAdapter(mcpClient, mcpTool)

                if (verbose) {
                    println("[MCP Discovery] Registered tool: ${mcpTool.name} - ${mcpTool.description}")
                }

                adapter
            }

            if (verbose) {
                println("[MCP Discovery] Tool discovery complete!")
            }

            koogTools
        } catch (e: McpException) {
            println("[MCP Discovery] Failed to discover tools: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
            emptyList()
        } catch (e: Exception) {
            println("[MCP Discovery] Unexpected error during tool discovery: ${e.message}")
            if (verbose) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    /**
     * Test connection to MCP server
     */
    fun testConnection(): Boolean {
        return try {
            if (verbose) {
                println("[MCP Discovery] Testing connection...")
            }

            val available = mcpClient.isServerAvailable()

            if (verbose) {
                if (available) {
                    println("[MCP Discovery] Connection successful!")
                } else {
                    println("[MCP Discovery] Server not available")
                }
            }

            available
        } catch (e: Exception) {
            if (verbose) {
                println("[MCP Discovery] Connection test failed: ${e.message}")
            }
            false
        }
    }
}
