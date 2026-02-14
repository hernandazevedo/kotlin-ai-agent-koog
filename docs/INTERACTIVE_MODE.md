# Interactive Mode with Diff Viewer

This document explains the Interactive Mode feature, which allows you to review and approve file operations with a visual diff preview before they are executed.

## Overview

Interactive Mode provides three confirmation strategies:

1. **Brave Mode** (`--brave`): Auto-approves all operations (useful for automation)
2. **Interactive Mode** (`--interactive`): Shows diff and prompts for approval
3. **Safe Mode** (default): Uses safe confirmation handler (currently auto-approves)

## Usage

### Enable Interactive Mode

```bash
./gradlew jvmRun --args="/path/to/project 'Your task' --interactive"
```

### Example Session

```bash
./gradlew jvmRun --args="example-project 'Add error handling to Calculator' --interactive"
```

## Interactive Prompt

When the agent wants to modify a file, you'll see:

```
======================================================================
⚠️  FILE OPERATION REQUIRES APPROVAL
======================================================================
📁 File: src/main/kotlin/Calculator.kt
🔧 Action: OVERWRITE existing file

📝 FILE CHANGES
──────────────────────────────────────────────────────────────────────
📊 +3 -0 ~1
──────────────────────────────────────────────────────────────────────
     1 | package sample
     2 |
     3 | class Calculator {
-    4 |     fun add(a: Int, b: Int): Int {
+    4 |     fun add(a: Int, b: Int): Int? {
+    5 |         if (a < 0 || b < 0) return null
     6 |         return a + b
     7 |     }
──────────────────────────────────────────────────────────────────────

Options:
  [y] Yes    - Approve this operation
  [n] No     - Reject this operation
  [a] Always - Approve all operations (brave mode)
  [d] Deny   - Reject all operations
  [v] View   - Show full diff again
  [?] Help   - Show help

Your choice: _
```

## Commands

| Command | Description |
|---------|-------------|
| `y` or `yes` | Approve this single operation |
| `n` or `no` | Reject this single operation |
| `a` or `always` | Switch to brave mode (auto-approve everything) |
| `d` or `deny` | Reject all remaining operations |
| `v` or `view` | Show full file diff |
| `?` or `help` | Show help message |

## Diff Format

The diff viewer uses a color-coded unified diff format:

- **Green lines** (`+`): Lines being added
- **Red lines** (`-`): Lines being removed
- **White lines** (` `): Context lines (unchanged)
- **Cyan** (`📊`): Statistics line showing additions/deletions

### Statistics

The format `📊 +3 -0 ~1` means:
- `+3`: 3 lines added
- `-0`: 0 lines deleted
- `~1`: 1 line modified

## Use Cases

### 1. Code Review

Review AI-generated code changes before applying them:

```bash
./gradlew jvmRun --args="my-project 'Refactor UserService' --interactive"
```

### 2. Learning

Understand what the agent is doing by reviewing each change:

```bash
./gradlew jvmRun --args="my-project 'Add validation' --interactive"
```

### 3. Safety

Prevent unwanted changes in sensitive files:

```bash
./gradlew jvmRun --args="production-code 'Fix bug' --interactive"
```

## Features

### New File Creation

When creating a new file, you'll see a preview:

```
📄 NEW FILE
──────────────────────────────────────────────────────────────────────
+ package com.example
+
+ class NewFeature {
+     fun doSomething() {
+         // Implementation
+     }
+ }
──────────────────────────────────────────────────────────────────────
Total: 7 lines
```

### File Editing

When editing an existing file, you'll see the actual diff:

```
📝 FILE CHANGES
──────────────────────────────────────────────────────────────────────
📊 +2 -1 ~0
──────────────────────────────────────────────────────────────────────
  10 |  fun calculate() {
- 11 |      return result
+ 11 |      return result ?: 0
+ 12 |      // Added null safety
  13 |  }
──────────────────────────────────────────────────────────────────────
```

## Advanced Usage

### Combining with User Tracking

```bash
./gradlew jvmRun --args="project 'task' --interactive --user developer@example.com"
```

### Switching Modes Mid-Session

During interactive mode, you can:
- Press `a` to switch to brave mode (stop prompting)
- Press `d` to reject all remaining operations

## Implementation Details

### Components

1. **DiffViewer.kt**: Generates unified diffs using LCS algorithm
2. **InteractiveConfirmationHandler.kt**: Handles user prompts and input
3. **ConfirmationHandler interface**: Extended to support content preview

### Algorithm

The diff viewer uses a **Longest Common Subsequence (LCS)** algorithm to compute line-by-line changes, similar to `git diff`.

### Color Support

ANSI color codes are used for terminal output:
- Green: `\u001B[32m`
- Red: `\u001B[31m`
- Cyan: `\u001B[36m`
- Gray: `\u001B[90m`

Colors can be disabled by passing `useColors = false` to the handler.

## Troubleshooting

### Colors Not Showing

If colors aren't displaying correctly:
1. Check that your terminal supports ANSI colors
2. Try a different terminal (e.g., iTerm2 on macOS)
3. Or disable colors in the code

### Prompt Not Appearing

If you don't see the interactive prompt:
1. Make sure you're using `--interactive` flag
2. Check that you're not also using `--brave`
3. Verify stdin is connected (doesn't work in some CI environments)

## Comparison with Other Modes

| Feature | Brave | Interactive | Safe |
|---------|-------|-------------|------|
| Shows Diff | ❌ | ✅ | ❌ |
| Prompts User | ❌ | ✅ | ❌ |
| Color Output | ❌ | ✅ | ❌ |
| Auto-Approve | ✅ | Optional | ✅ |
| CI/CD Friendly | ✅ | ❌ | ✅ |

## Future Enhancements

Potential improvements:
- Web UI for remote approval
- IDE plugin integration (see IDE_INTEGRATION.md)
- Rules-based auto-approval
- Session recording and replay
- Partial approval (approve some hunks, reject others)

## Related Documentation

- [Sub-Agents](SUBAGENTS.md)
- [Main README](../README.md)
