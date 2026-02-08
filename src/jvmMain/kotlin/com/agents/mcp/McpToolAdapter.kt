package com.agents.mcp

import ai.koog.agents.core.tools.SimpleTool
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Adapter that bridges MCP tools to Koog's SimpleTool interface
 */
class McpToolAdapter(
    private val mcpClient: McpClient,
    private val mcpTool: McpTool
) : SimpleTool<McpToolAdapter.Args>(
    name = mcpTool.name,
    description = mcpTool.description,
    argsSerializer = Args.serializer()
) {
    /**
     * Arguments wrapper for MCP tool calls
     * This accepts any JSON object as the MCP tool schema is dynamic
     */
    @Serializable
    data class Args(
        val arguments: Map<String, @Serializable(with = JsonElementSerializer::class) JsonElement>
    )

    override suspend fun execute(args: Args): String {
        return try {
            // Convert JsonElement map to Any map for MCP client
            val mcpArgs = args.arguments.mapValues { (_, value) ->
                jsonElementToAny(value)
            }

            val result = mcpClient.callTool(mcpTool.name, mcpArgs)

            if (result.isError) {
                buildErrorResult(result.content.firstOrNull()?.text ?: "Unknown error from MCP server")
            } else {
                buildSuccessResult(result.content)
            }
        } catch (e: McpException) {
            buildErrorResult("MCP protocol error: ${e.message}")
        } catch (e: Exception) {
            buildErrorResult("Failed to execute tool '${mcpTool.name}': ${e.message ?: "Unknown error"}")
        }
    }

    private fun buildSuccessResult(content: List<TextContent>): String {
        return content.joinToString("\n") { it.text }
    }

    private fun buildErrorResult(message: String): String {
        return """
            Error: $message

            Suggestion:
            - Verify the MCP server is running and accessible
            - Check that the tool parameters are correct
            - Review the tool's expected input schema
        """.trimIndent()
    }

    /**
     * Convert JsonElement to Any for MCP client
     */
    private fun jsonElementToAny(element: JsonElement): Any {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                element.content
            }
            is kotlinx.serialization.json.JsonArray -> {
                element.map { jsonElementToAny(it) }
            }
            is JsonObject -> {
                element.mapValues { jsonElementToAny(it.value) }
            }
        }
    }
}

/**
 * Custom serializer for JsonElement to handle dynamic JSON schemas
 */
object JsonElementSerializer : KSerializer<JsonElement> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: JsonElement) {
        JsonElement.serializer().serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): JsonElement {
        return JsonElement.serializer().deserialize(decoder)
    }
}
