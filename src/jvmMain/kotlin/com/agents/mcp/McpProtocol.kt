package com.agents.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class InitializeResult(
    val protocolVersion: String,
    val capabilities: Capabilities,
    val serverInfo: ServerInfo
)

@Serializable
data class Capabilities(
    val tools: ToolsCapability? = null
)

@Serializable
data class ToolsCapability(
    val listChanged: Boolean = false
)

@Serializable
data class ToolListResult(
    val tools: List<McpTool>
)

@Serializable
data class ToolCallResult(
    val content: List<TextContent>,
    val isError: Boolean = false
)

@Serializable
data class TextContent(
    val type: String = "text",
    val text: String
)
