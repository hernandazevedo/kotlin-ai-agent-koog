# Memory Management and History Compression

This document describes the memory management and history compression implementation in this project, based on [JetBrains Koog blog post Part 5: Teaching Agents to Forget](https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-5-teaching-agents-to-forget/).

## Overview

As AI agents work on complex tasks, they accumulate conversation history that eventually exceeds the LLM's context window. Memory management solves this by:

- **Compressing history** when it becomes too large
- **Preserving critical information** while discarding less important details
- **Reducing costs** by minimizing token usage
- **Maintaining context** across long-running tasks

## The Problem

Without memory management:
- Agents run out of context space
- Complex tasks can't be completed
- API costs increase unnecessarily
- Performance degrades with long conversations

Typical limits:
- GPT-4: ~8K tokens (32K characters)
- GPT-4-32K: ~32K tokens (128K characters)
- Our default threshold: 200 messages or 200K characters (~50K tokens)

## Compression Strategies

### 1. Whole History Summarization

Compresses the entire conversation into a TL;DR summary.

**Pros:**
- Simple to implement
- Preserves general context
- Fast compression

**Cons:**
- Less precise
- May lose important details
- Hard to control what's preserved

**Use case:** General assistants where broad context is more important than specific details.

### 2. Retrieve Specific Facts (Recommended for Code Agents)

Asks the LLM targeted questions about critical information to extract and preserve.

**Pros:**
- More controlled and reliable
- Preserves exactly what you need
- Better for structured tasks

**Cons:**
- Requires defining concepts upfront
- Slightly more complex setup

**Use case:** Code generation, project management, task-oriented agents.

## Implementation

### Configuration

Location: `src/jvmMain/kotlin/com/agents/memory/HistoryCompressionConfig.kt`

#### Concepts for Code Agents

We define 5 key concepts to preserve:

```kotlin
object AgentConcepts {
    const val PROJECT_STRUCTURE = "project-structure"
    const val ACHIEVEMENTS = "important-achievements"
    const val CURRENT_CONTEXT = "current-context"
    const val ERRORS_AND_FIXES = "errors-and-fixes"
    const val USER_REQUIREMENTS = "user-requirements"
}
```

Each concept maps to a specific question:

```kotlin
Concept(
    id = AgentConcepts.PROJECT_STRUCTURE,
    question = "What is the project structure? List main directories, key files, and technologies used.",
    factType = FactType.MULTIPLE
)
```

#### Compression Triggers

Compression is triggered when:

```kotlin
CompressionTriggerConfig(
    maxMessages = 200,        // More than 200 messages
    maxCharacters = 200_000   // OR more than 200K characters
)
```

You can also provide a custom predicate:

```kotlin
CompressionTriggerConfig(
    customPredicate = { messageCount, totalChars ->
        messageCount > 150 && totalChars > 100_000
    }
)
```

### Usage

#### Default Configuration (Recommended)

```kotlin
val compressionConfig = HistoryCompressionConfig.forCodeAgent()
```

This creates a configuration optimized for code generation agents with:
- 200 message threshold
- 200K character threshold
- 5 predefined concepts for code work
- Memory preservation enabled

#### Custom Configuration

```kotlin
val compressionConfig = HistoryCompressionConfig(
    trigger = CompressionTriggerConfig(
        maxMessages = 150,
        maxCharacters = 150_000
    ),
    strategy = CompressionStrategy.RetrieveFacts(
        concepts = listOf(
            Concept(
                id = "my-custom-concept",
                question = "What custom information should be preserved?",
                factType = FactType.SINGLE
            )
        )
    ),
    preserveMemory = true,
    enabled = true
)
```

#### Disable Compression

```kotlin
val compressionConfig = HistoryCompressionConfig.disabled()
```

### Integration with AIAgent

History compression can be integrated using Koog's strategy system:

```kotlin
// In a strategy graph
val compressHistory by nodeLLMCompressHistory<Input>(
    strategy = HistoryCompressionStrategy.WholeHistory
)

// Or in a custom node
llm.writeSession {
    if (shouldCompress(prompt.messages)) {
        replaceHistoryWithTLDR(
            strategy = HistoryCompressionStrategy.RetrieveFactsFromHistory(
                concepts = compressionConfig.strategy.concepts
            )
        )
    }
}
```

## How It Works

### Before Compression

```
Message 1: "Read file A.kt"
Message 2: [Tool Result: File contents...]
Message 3: "Modify function X"
Message 4: [Tool Result: Success]
...
Message 200: "Current task..."
```

Total: 200 messages, 180K characters

### After Compression (Retrieve Facts Strategy)

```
System: Original system prompt

Preserved Facts:
- Project Structure: Kotlin project with Main.kt, tools package, MCP integration
- Achievements: Modified function X in A.kt, added feature Y
- Current Context: Working on adding error handling to file operations
- Errors/Fixes: Fixed compilation error in line 42 by adding type annotation
- User Requirements: Use confirmation handlers, preserve brave mode functionality

Recent Messages (last 10):
Message 191-200...
```

Total: ~20 messages, 30K characters

## Benefits Observed

Based on JetBrains blog and Koog best practices:

- **60-80% reduction** in context size
- **Maintains task continuity** across long sessions
- **Reduces API costs** by minimizing token usage
- **Improves performance** by reducing prompt processing time
- **Enables complex tasks** that would otherwise exceed context limits

## Example Scenario

### Without Compression

```
Agent: "I've run out of context space. I can't remember what we did 100 messages ago."
[Agent fails to complete task]
```

### With Compression

```
[At message 200, compression triggers]

Agent compresses history:
- Project Structure: ✓ Preserved
- Achievements: ✓ Preserved
- Current Context: ✓ Preserved
- Old tool calls: ✗ Removed
- Verbose outputs: ✗ Summarized

Agent continues working with reduced context:
"Based on the project structure and previous achievements,
 I'll now implement the error handling as requested..."

[Agent successfully completes task]
```

## Configuration Best Practices

### For Short Tasks (< 50 messages)

```kotlin
HistoryCompressionConfig.disabled()
```

No compression needed - keeps full history for debugging.

### For Medium Tasks (50-200 messages)

```kotlin
HistoryCompressionConfig.forCodeAgent(
    maxMessages = 100,
    maxCharacters = 100_000
)
```

Aggressive compression to maintain performance.

### For Long Tasks (> 200 messages)

```kotlin
HistoryCompressionConfig.forCodeAgent(
    maxMessages = 200,
    maxCharacters = 200_000
)
```

Balanced approach - compress when necessary but not too frequently.

### For Custom Domains

Define domain-specific concepts:

```kotlin
HistoryCompressionConfig(
    strategy = CompressionStrategy.RetrieveFacts(
        concepts = listOf(
            Concept("database-schema", "What is the current database schema?"),
            Concept("api-endpoints", "What API endpoints have been created?"),
            Concept("test-coverage", "What test coverage has been achieved?")
        )
    )
)
```

## Monitoring Compression

Add logging to track compression events:

```kotlin
if (compressionConfig.trigger.shouldCompress(messageCount, totalChars)) {
    println("[Memory] Triggering compression: $messageCount messages, $totalChars chars")
    // Perform compression
    println("[Memory] Compression complete: ${newMessageCount} messages, ${newTotalChars} chars")
}
```

## Future Enhancements

Potential improvements:

1. **Semantic Compression**: Use embeddings to identify similar messages
2. **Priority-based Retention**: Keep messages with high importance scores
3. **Incremental Compression**: Compress old messages while keeping recent ones
4. **Multi-tier Memory**: Short-term, medium-term, and long-term memory layers
5. **Cross-session Memory**: Persist important facts across agent restarts

## References

- [Building AI Agents in Kotlin - Part 5: Teaching Agents to Forget](https://blog.jetbrains.com/ai/2026/01/building-ai-agents-in-kotlin-part-5-teaching-agents-to-forget/)
- [Koog History Compression Documentation](https://docs.koog.ai/history-compression/)
- [Koog Memory Features](https://docs.koog.ai/)
