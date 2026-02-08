package com.agents.mcp

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testJsonRpcRequest_serialization() {
        val request = JsonRpcRequest(
            method = "test/method",
            params = buildJsonObject {
                put("key", "value")
            }
        )

        assertEquals("2.0", request.jsonrpc)
        assertEquals("test/method", request.method)
        assertEquals("value", request.params?.get("key")?.jsonPrimitive?.content)
    }

    @Test
    fun testJsonRpcResponse_success() {
        val response = JsonRpcResponse(
            result = JsonPrimitive("success")
        )

        assertEquals("2.0", response.jsonrpc)
        assertEquals("success", response.result?.jsonPrimitive?.content)
        assertEquals(null, response.error)
    }

    @Test
    fun testJsonRpcResponse_error() {
        val response = JsonRpcResponse(
            error = JsonRpcError(
                code = -32600,
                message = "Invalid Request"
            )
        )

        assertEquals(-32600, response.error?.code)
        assertEquals("Invalid Request", response.error?.message)
        assertEquals(null, response.result)
    }

    @Test
    fun testJsonRpcError() {
        val error = JsonRpcError(
            code = -32700,
            message = "Parse error",
            data = JsonPrimitive("Additional info")
        )

        assertEquals(-32700, error.code)
        assertEquals("Parse error", error.message)
        assertEquals("Additional info", error.data?.jsonPrimitive?.content)
    }

    @Test
    fun testMcpTool() {
        val tool = McpTool(
            name = "test__tool",
            description = "A test tool",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("arg1", buildJsonObject {
                        put("type", "string")
                    })
                })
            }
        )

        assertEquals("test__tool", tool.name)
        assertEquals("A test tool", tool.description)
        assertEquals("object", tool.inputSchema["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun testServerInfo() {
        val serverInfo = ServerInfo(
            name = "test-server",
            version = "1.0.0"
        )

        assertEquals("test-server", serverInfo.name)
        assertEquals("1.0.0", serverInfo.version)
    }

    @Test
    fun testInitializeResult() {
        val result = InitializeResult(
            protocolVersion = "2024-11-05",
            capabilities = Capabilities(
                tools = ToolsCapability(listChanged = true)
            ),
            serverInfo = ServerInfo(
                name = "test-server",
                version = "1.0.0"
            )
        )

        assertEquals("2024-11-05", result.protocolVersion)
        assertEquals(true, result.capabilities.tools?.listChanged)
        assertEquals("test-server", result.serverInfo.name)
    }

    @Test
    fun testCapabilities() {
        val capabilities = Capabilities(
            tools = ToolsCapability(listChanged = false)
        )

        assertEquals(false, capabilities.tools?.listChanged)
    }

    @Test
    fun testCapabilities_noTools() {
        val capabilities = Capabilities()

        assertEquals(null, capabilities.tools)
    }

    @Test
    fun testToolsCapability() {
        val capability = ToolsCapability(listChanged = true)

        assertTrue(capability.listChanged)
    }

    @Test
    fun testToolsCapability_default() {
        val capability = ToolsCapability()

        assertFalse(capability.listChanged)
    }

    @Test
    fun testToolListResult() {
        val result = ToolListResult(
            tools = listOf(
                McpTool(
                    name = "tool1",
                    description = "First tool",
                    inputSchema = buildJsonObject { }
                ),
                McpTool(
                    name = "tool2",
                    description = "Second tool",
                    inputSchema = buildJsonObject { }
                )
            )
        )

        assertEquals(2, result.tools.size)
        assertEquals("tool1", result.tools[0].name)
        assertEquals("tool2", result.tools[1].name)
    }

    @Test
    fun testToolCallResult_success() {
        val result = ToolCallResult(
            content = listOf(
                TextContent(text = "Operation successful")
            ),
            isError = false
        )

        assertEquals(1, result.content.size)
        assertEquals("Operation successful", result.content[0].text)
        assertFalse(result.isError)
    }

    @Test
    fun testToolCallResult_error() {
        val result = ToolCallResult(
            content = listOf(
                TextContent(text = "Error occurred")
            ),
            isError = true
        )

        assertEquals("Error occurred", result.content[0].text)
        assertTrue(result.isError)
    }

    @Test
    fun testTextContent() {
        val content = TextContent(text = "Hello, MCP!")

        assertEquals("text", content.type)
        assertEquals("Hello, MCP!", content.text)
    }

    @Test
    fun testTextContent_multipleItems() {
        val contents = listOf(
            TextContent(text = "Line 1"),
            TextContent(text = "Line 2"),
            TextContent(text = "Line 3")
        )

        assertEquals(3, contents.size)
        assertEquals("Line 1", contents[0].text)
        assertEquals("Line 2", contents[1].text)
        assertEquals("Line 3", contents[2].text)
    }
}
