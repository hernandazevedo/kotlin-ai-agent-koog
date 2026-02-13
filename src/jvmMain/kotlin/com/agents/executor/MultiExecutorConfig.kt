package com.agents.executor

import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider

/**
 * Multi-Executor Configuration for AI Agents
 *
 * Based on JetBrains Koog example: step-05-history
 * https://github.com/JetBrains/koog/tree/main/examples/code-agent/step-05-history
 *
 * MultiLLMPromptExecutor allows using multiple LLM providers:
 * - Primary model for main agent operations (e.g., Anthropic Claude)
 * - Secondary model for compression/retrieval (e.g., OpenAI GPT-4o-mini)
 *
 * Benefits:
 * - Use the best model for each task
 * - Cost optimization (cheaper model for compression)
 * - Fallback support if one provider is unavailable
 * - Provider diversity reduces vendor lock-in
 */

/**
 * Available LLM provider configurations
 */
sealed class ExecutorConfig {
    /**
     * Use only OpenAI (GPT-4o)
     * Single provider, simpler setup
     */
    data class OpenAIOnly(val apiKey: String) : ExecutorConfig()

    /**
     * Use only Anthropic (Claude)
     * Single provider, simpler setup
     */
    data class AnthropicOnly(val apiKey: String) : ExecutorConfig()

    /**
     * Use multiple providers with MultiLLMPromptExecutor
     * - Primary: Anthropic Claude Sonnet for main operations
     * - Secondary: OpenAI GPT-4o-mini for history compression
     */
    data class Multi(
        val openAIKey: String,
        val anthropicKey: String? = null
    ) : ExecutorConfig()
}

/**
 * Model configuration for the agent
 */
data class ModelConfig(
    /**
     * Primary model for agent operations
     */
    val primaryModel: Any, // OpenAIModels.Chat.* or AnthropicModels.*

    /**
     * Model for history compression and fact retrieval
     * Usually a cheaper/faster model since compression is a simpler task
     */
    val retrievalModel: Any? = null // Optional cheaper model for compression
)

/**
 * Create PromptExecutor based on configuration
 * Now uses real MultiLLMPromptExecutor for multi-provider support (Koog 0.6.2+)
 */
fun createPromptExecutor(config: ExecutorConfig): PromptExecutor {
    return when (config) {
        is ExecutorConfig.OpenAIOnly -> {
            OpenAILLMClient(config.apiKey) as PromptExecutor
        }

        is ExecutorConfig.AnthropicOnly -> {
            AnthropicLLMClient(config.apiKey) as PromptExecutor
        }

        is ExecutorConfig.Multi -> {
            // Create MultiLLMPromptExecutor with both providers
            // Based on Koog example: step-05-history
            MultiLLMPromptExecutor(
                LLMProvider.OpenAI to OpenAILLMClient(config.openAIKey),
                *(config.anthropicKey?.let { key ->
                    arrayOf(LLMProvider.Anthropic to AnthropicLLMClient(key))
                } ?: emptyArray())
            )
        }
    }
}

/**
 * Default model configurations
 */
object DefaultModels {
    /**
     * OpenAI-only configuration
     * Primary: GPT-4o for main operations
     * Retrieval: GPT-4o-mini for compression (cheaper)
     */
    val openAI = ModelConfig(
        primaryModel = OpenAIModels.Chat.GPT4o,
        retrievalModel = OpenAIModels.Chat.GPT4oMini
    )

    /**
     * Anthropic-only configuration
     * Primary: Claude Sonnet 4.5 for main operations
     * Retrieval: Uses same model (no cheaper alternative currently)
     */
    val anthropic = ModelConfig(
        primaryModel = AnthropicModels.Sonnet_4_5,
        retrievalModel = null // Use same model
    )

    /**
     * Multi-provider configuration (recommended)
     * Primary: Claude Sonnet 4.5 (best reasoning)
     * Retrieval: GPT-4o-mini (cheap and fast)
     *
     * This is the optimal setup from the Koog example:
     * - Claude for complex code generation
     * - GPT-4o-mini for history compression
     */
    val multi = ModelConfig(
        primaryModel = AnthropicModels.Sonnet_4_5,
        retrievalModel = OpenAIModels.Chat.GPT4oMini
    )
}

/**
 * Helper to determine executor config from environment
 * Uses provided function to read environment (supports .env files)
 */
fun getExecutorConfigFromEnv(getEnv: (String) -> String? = System::getenv): ExecutorConfig {
    val openAIKey = getEnv("OPENAI_API_KEY")
    val anthropicKey = getEnv("ANTHROPIC_API_KEY")

    return when {
        // Both keys available - use multi-provider (optimal)
        openAIKey != null && anthropicKey != null -> {
            println("[Executor] Using Multi-Provider with OpenAI + Anthropic")
            ExecutorConfig.Multi(openAIKey, anthropicKey)
        }

        // Only OpenAI key available
        openAIKey != null -> {
            println("[Executor] Using OpenAI only")
            ExecutorConfig.OpenAIOnly(openAIKey)
        }

        // Only Anthropic key available
        anthropicKey != null -> {
            println("[Executor] Using Anthropic only")
            ExecutorConfig.AnthropicOnly(anthropicKey)
        }

        // No keys available
        else -> {
            throw IllegalStateException(
                "No LLM API keys found. Set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variable."
            )
        }
    }
}

/**
 * Get model configuration based on executor config
 */
fun getModelConfig(executorConfig: ExecutorConfig): ModelConfig {
    return when (executorConfig) {
        is ExecutorConfig.OpenAIOnly -> DefaultModels.openAI
        is ExecutorConfig.AnthropicOnly -> DefaultModels.anthropic
        is ExecutorConfig.Multi -> {
            // If Anthropic key is available, use multi-provider config
            if (executorConfig.anthropicKey != null) {
                DefaultModels.multi
            } else {
                // Otherwise just OpenAI
                DefaultModels.openAI
            }
        }
    }
}
