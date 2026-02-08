package com.agents.mcp

import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

class McpClient(
    private val serverUrl: String = "http://localhost:8080/mcp",
    private val verbose: Boolean = false
) {
    private val httpClient = OkHttpClient()
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val requestIdCounter = AtomicLong(1)

    /**
     * Initialize connection with MCP server
     */
    fun initialize(): InitializeResult {
        val request = JsonRpcRequest(
            id = JsonPrimitive(requestIdCounter.getAndIncrement()),
            method = "initialize",
            params = buildJsonObject {}
        )

        val response = sendRequest(request)

        if (response.error != null) {
            throw McpException("Failed to initialize: ${response.error.message}")
        }

        if (response.result == null) {
            throw McpException("Initialize response missing result")
        }

        return json.decodeFromJsonElement(response.result)
    }

    /**
     * List all available tools from MCP server
     */
    fun listTools(): List<McpTool> {
        val request = JsonRpcRequest(
            id = JsonPrimitive(requestIdCounter.getAndIncrement()),
            method = "tools/list",
            params = null
        )

        val response = sendRequest(request)

        if (response.error != null) {
            throw McpException("Failed to list tools: ${response.error.message}")
        }

        if (response.result == null) {
            throw McpException("List tools response missing result")
        }

        val toolListResult: ToolListResult = json.decodeFromJsonElement(response.result)
        return toolListResult.tools
    }

    /**
     * Call a tool on the MCP server
     */
    fun callTool(name: String, arguments: Map<String, Any>): ToolCallResult {
        val argumentsJson = buildJsonObject {
            arguments.forEach { (key, value) ->
                put(key, convertToJsonElement(value))
            }
        }

        val request = JsonRpcRequest(
            id = JsonPrimitive(requestIdCounter.getAndIncrement()),
            method = "tools/call",
            params = buildJsonObject {
                put("name", name)
                put("arguments", argumentsJson)
            }
        )

        val response = sendRequest(request)

        if (response.error != null) {
            throw McpException("Failed to call tool '$name': ${response.error.message}")
        }

        if (response.result == null) {
            throw McpException("Tool call response missing result")
        }

        return json.decodeFromJsonElement(response.result)
    }

    /**
     * Send JSON-RPC request to MCP server via HTTP
     */
    private fun sendRequest(jsonRpcRequest: JsonRpcRequest): JsonRpcResponse {
        val requestBody = json.encodeToString(JsonRpcRequest.serializer(), jsonRpcRequest)

        if (verbose) {
            println("[MCP Client] Request: $requestBody")
        }

        val httpRequest = Request.Builder()
            .url(serverUrl)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string()
                    ?: throw McpException("Empty response from server")

                if (verbose) {
                    println("[MCP Client] Response: $responseBody")
                }

                if (!response.isSuccessful) {
                    throw McpException("HTTP error: ${response.code} - $responseBody")
                }

                return json.decodeFromString(JsonRpcResponse.serializer(), responseBody)
            }
        } catch (e: IOException) {
            throw McpException("Network error: ${e.message}", e)
        }
    }

    /**
     * Convert Kotlin types to JsonElement
     */
    private fun convertToJsonElement(value: Any): JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) ->
                    if (k is String && v != null) {
                        put(k, convertToJsonElement(v))
                    }
                }
            }
            is List<*> -> buildJsonArray {
                value.forEach { item ->
                    if (item != null) {
                        add(convertToJsonElement(item))
                    }
                }
            }
            else -> JsonPrimitive(value.toString())
        }
    }

    /**
     * Check if MCP server is reachable
     */
    fun isServerAvailable(): Boolean {
        return try {
            val healthCheckUrl = serverUrl.replace("/mcp", "/health")
            val request = Request.Builder()
                .url(healthCheckUrl)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}

class McpException(message: String, cause: Throwable? = null) : Exception(message, cause)
