# Multi-Executor Configuration

This document describes the Multi-Executor pattern for using multiple LLM providers, based on the [Koog example: step-05-history](https://github.com/JetBrains/koog/tree/main/examples/code-agent/step-05-history).

## Overview

The Multi-Executor pattern allows you to use multiple LLM providers in a single agent, enabling:

- **Best model for each task**: Use Claude for complex reasoning, GPT-4o-mini for compression
- **Cost optimization**: Cheaper models for simpler tasks like history compression
- **Provider diversity**: Reduces vendor lock-in
- **Fallback support**: Continue working if one provider is unavailable

## Architecture

```
┌─────────────────────────────────────┐
│         AIAgent                     │
│  ┌───────────────────────────────┐ │
│  │   MultiLLMPromptExecutor      │ │
│  │  ┌─────────┐   ┌───────────┐ │ │
│  │  │ OpenAI  │   │ Anthropic │ │ │
│  │  │ GPT-4o  │   │  Claude   │ │ │
│  │  └─────────┘   └───────────┘ │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘

Primary Task → Claude Sonnet 4.5
History Compression → GPT-4o-mini
```

## Implementation

Location: `src/jvmMain/kotlin/com/agents/executor/MultiExecutorConfig.kt`

### Executor Configurations

#### 1. OpenAI Only (Simple)

```kotlin
val config = ExecutorConfig.OpenAIOnly(
    apiKey = System.getenv("OPENAI_API_KEY")!!
)

val client = createLLMClient(config)
```

**Models:**
- Primary: GPT-4o
- Compression: GPT-4o-mini

**Use when:** You only have OpenAI API access.

#### 2. Anthropic Only

```kotlin
val config = ExecutorConfig.AnthropicOnly(
    apiKey = System.getenv("ANTHROPIC_API_KEY")!!
)

val client = createLLMClient(config)
```

**Models:**
- Primary: Claude Sonnet 4.5
- Compression: Same (no cheaper alternative)

**Use when:** You only have Anthropic API access.

#### 3. Multi-Provider (Recommended)

```kotlin
val config = ExecutorConfig.Multi(
    openAIKey = System.getenv("OPENAI_API_KEY")!!,
    anthropicKey = System.getenv("ANTHROPIC_API_KEY")
)

val client = createLLMClient(config)
```

**Models:**
- Primary: Claude Sonnet 4.5 (best reasoning for code)
- Compression: GPT-4o-mini (cheap and fast)

**Use when:** You want optimal performance and cost balance.

### Model Configuration

```kotlin
// Get model config based on available API keys
val modelConfig = getModelConfig(executorConfig)

// Create agent with primary model
val agent = AIAgent(
    promptExecutor = client,
    llmModel = modelConfig.primaryModel,
    // ... other config
)

// Use retrieval model for history compression
val compressionConfig = HistoryCompressionConfig(
    retrievalModel = modelConfig.retrievalModel
)
```

### Auto-Detection from Environment

The helper function automatically detects available API keys:

```kotlin
val executorConfig = getExecutorConfigFromEnv()
// [Executor] Using MultiLLMPromptExecutor with OpenAI + Anthropic

val modelConfig = getModelConfig(executorConfig)
val client = createLLMClient(executorConfig)
```

**Priority:**
1. If both keys available → Multi-provider (optimal)
2. If only OpenAI key → OpenAI only
3. If only Anthropic key → Anthropic only
4. If no keys → Error

## Benefits

### 1. Cost Optimization

**Without Multi-Executor:**
```
Main Task (200 messages):     Claude Sonnet 4.5 @ $15/1M tokens
History Compression (50x):    Claude Sonnet 4.5 @ $15/1M tokens
Total Cost: ~$0.75 per session
```

**With Multi-Executor:**
```
Main Task (200 messages):     Claude Sonnet 4.5 @ $15/1M tokens
History Compression (50x):    GPT-4o-mini @ $0.15/1M tokens
Total Cost: ~$0.45 per session
```

**Savings: ~40% on compression operations**

### 2. Best Model for Each Task

| Task | Best Model | Why |
|------|------------|-----|
| Code Generation | Claude Sonnet 4.5 | Superior reasoning, better code quality |
| Tool Usage | Claude Sonnet 4.5 | Better at following complex instructions |
| History Compression | GPT-4o-mini | Fast, cheap, good enough for summarization |
| Fact Extraction | GPT-4o-mini | Simple Q&A task, no complex reasoning needed |

### 3. Provider Diversity

```kotlin
// Primary provider issues? Swap easily:
val config = ExecutorConfig.Multi(
    openAIKey = openAIKey,
    anthropicKey = null  // Disable Anthropic
)
// Now uses OpenAI for everything
```

## Integration with History Compression

From the Koog example:

```kotlin
val agent = AIAgent(
    promptExecutor = multiExecutor,
    llmModel = AnthropicModels.Sonnet_4_5,  // Primary model
    strategy = singleRunStrategyWithHistoryCompression(
        config = HistoryCompressionConfig(
            isHistoryTooBig = { prompt ->
                prompt.messages.size > 200 ||
                prompt.messages.sumOf { it.content.length } > 200_000
            },
            compressionStrategy = RetrieveFactsFromHistory(...),
            retrievalModel = OpenAIModels.Chat.GPT4_1Mini  // Cheap model
        )
    ),
    maxIterations = 400
)
```

**How it works:**
1. Agent runs with Claude Sonnet 4.5 for main operations
2. When history exceeds threshold, compression triggers
3. GPT-4o-mini extracts facts from history (cheaper)
4. Compressed history replaces old messages
5. Agent continues with Claude and reduced context

## Example Usage

### Basic Setup

```kotlin
// In Main.kt
fun main() {
    // Auto-detect available providers
    val executorConfig = getExecutorConfigFromEnv()
    val modelConfig = getModelConfig(executorConfig)
    val executor = createLLMClient(executorConfig)

    // Create agent
    val agent = AIAgent(
        promptExecutor = executor,
        llmModel = modelConfig.primaryModel,
        toolRegistry = ToolRegistry { /* tools */ }
    )

    agent.use { it.run("Your task") }
}
```

### With History Compression

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    llmModel = modelConfig.primaryModel,
    strategy = singleRunStrategyWithHistoryCompression(
        config = HistoryCompressionConfig.forCodeAgent(
            retrievalModel = modelConfig.retrievalModel
        )
    )
)
```

## Environment Variables

Set these in your `.env` file or environment:

```bash
# OpenAI (required for multi-provider or OpenAI-only)
OPENAI_API_KEY=sk-...

# Anthropic (optional, enables multi-provider)
ANTHROPIC_API_KEY=sk-ant-...
```

## Cost Comparison

Based on typical code agent session (500 total messages, 5 compressions):

| Configuration | Main Tasks | Compression | Total Cost |
|---------------|------------|-------------|------------|
| Claude Only | $2.50 | $0.50 | **$3.00** |
| OpenAI Only | $1.00 | $0.05 | **$1.05** |
| Multi-Provider | $2.50 | $0.05 | **$2.55** |

**Multi-Provider gives you:**
- Best quality (Claude for main tasks)
- Best price (GPT-4o-mini for compression)
- **15% cheaper than Claude-only**
- **Better quality than OpenAI-only**

## Best Practices

### 1. Use Multi-Provider in Production

```kotlin
// ✅ Good: Multi-provider with fallback
val config = getExecutorConfigFromEnv()

// ❌ Bad: Hard-coded single provider
val client = OpenAILLMClient(apiKey)
```

### 2. Monitor Provider Usage

```kotlin
println("[Executor] Using ${executorConfig::class.simpleName}")
println("[Model] Primary: ${modelConfig.primaryModel}")
println("[Model] Retrieval: ${modelConfig.retrievalModel ?: "same as primary"}")
```

### 3. Handle Missing Keys Gracefully

```kotlin
try {
    val config = getExecutorConfigFromEnv()
} catch (e: IllegalStateException) {
    println("Error: ${e.message}")
    println("Set OPENAI_API_KEY or ANTHROPIC_API_KEY")
    return
}
```

## Troubleshooting

### "No LLM API keys found"

**Cause:** Neither OPENAI_API_KEY nor ANTHROPIC_API_KEY is set.

**Fix:**
```bash
export OPENAI_API_KEY="sk-..."
# or
export ANTHROPIC_API_KEY="sk-ant-..."
```

### High Compression Costs

**Cause:** Using expensive model for compression.

**Fix:** Ensure retrieval model is set to a cheaper model:
```kotlin
val modelConfig = ModelConfig(
    primaryModel = AnthropicModels.Sonnet_4_5,
    retrievalModel = OpenAIModels.Chat.GPT4oMini  // ✅ Cheap
)
```

### Provider Not Available

**Cause:** API key invalid or quota exceeded.

**Fix:** Fallback to other provider:
```kotlin
val config = ExecutorConfig.OpenAIOnly(openAIKey)  // Use OpenAI instead
```

## Future Enhancements

Potential improvements:

1. **Dynamic Provider Selection**: Choose provider per request based on task type
2. **Load Balancing**: Distribute requests across providers
3. **Automatic Fallback**: Switch providers on error
4. **Cost Tracking**: Monitor spend per provider
5. **A/B Testing**: Compare quality across providers

## References

- [Koog Example: step-05-history](https://github.com/JetBrains/koog/tree/main/examples/code-agent/step-05-history)
- [Building AI Agents in Kotlin - Part 5: Teaching Agents to Forget](https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-5-teaching-agents-to-forget/)
- [Anthropic Claude Models](https://docs.anthropic.com/claude/docs/models-overview)
- [OpenAI Models](https://platform.openai.com/docs/models)
