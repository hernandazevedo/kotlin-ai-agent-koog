# 🎯 Interactive Mode Demo

This demo shows the Interactive Mode with Diff Viewer in action!

## 📋 Setup

```bash
# Make sure you have OPENAI_API_KEY configured
export OPENAI_API_KEY="your-key-here"
# or add it to .env file
```

## 🚀 Demo 1: Simple File Edit with Diff

Let's ask the agent to add error handling to the Calculator:

```bash
./gradlew jvmRun --args="example-project 'Add null safety to the add function in Calculator.kt' --interactive"
```

### Expected Output:

```
Starting AI Agent...
Project: example-project
Task: Add null safety to the add function in Calculator.kt
================================================================================
[Interactive Mode] Will prompt for approval with diff preview

... agent exploring files ...

======================================================================
⚠️  FILE OPERATION REQUIRES APPROVAL
======================================================================
📁 File: example-project/src/main/kotlin/sample/Calculator.kt
🔧 Action: OVERWRITE existing file

📝 FILE CHANGES
──────────────────────────────────────────────────────────────────────
📊 +3 -1 ~0
──────────────────────────────────────────────────────────────────────
     1 | package sample
     2 |
     3 | class Calculator {
-    4 |     fun add(a: Int, b: Int): Int {
+    4 |     fun add(a: Int, b: Int): Int? {
+    5 |         if (a < 0 || b < 0) return null
     5 |         return a + b
     6 |     }
     7 |
     8 |     fun subtract(a: Int, b: Int): Int {
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

### Interactive Commands:

#### Type `y` to approve:
```
Your choice: y
✅ Operation approved

✓ Successfully edited file: example-project/src/main/kotlin/sample/Calculator.kt
  Lines: 15 | Characters: 342

================================================================================
FINAL RESULT:
Successfully added null safety to the Calculator.kt add function.
The function now returns Int? and checks for negative inputs.
```

#### Type `v` to view full diff:
```
Your choice: v

──────────────────────────────────────────────────────────────────────
📄 FULL DIFF VIEW
──────────────────────────────────────────────────────────────────────
📝 FILE CHANGES
──────────────────────────────────────────────────────────────────────
📊 +3 -1 ~0
──────────────────────────────────────────────────────────────────────
     1 | package sample
     2 |
     3 | class Calculator {
-    4 |     fun add(a: Int, b: Int): Int {
+    4 |     fun add(a: Int, b: Int): Int? {
+    5 |         if (a < 0 || b < 0) return null
     5 |         return a + b
     6 |     }
     7 |
     8 |     fun subtract(a: Int, b: Int): Int {
     9 |         return a - b
    10 |     }
    11 |
    12 |     fun multiply(a: Int, b: Int): Int {
    13 |         return a * b
    14 |     }
    15 |
    16 |     fun divide(a: Int, b: Int): Int? {
    17 |         if (b == 0) return null
    18 |         return a / b
    19 |     }
    20 | }
──────────────────────────────────────────────────────────────────────

Your choice: _
```

#### Type `n` to reject:
```
Your choice: n
❌ Operation rejected

Operation rejected: File edit was not approved

================================================================================
FINAL RESULT:
Could not complete task because file edit was rejected by user.
```

## 🚀 Demo 2: Create New File

```bash
./gradlew jvmRun --args="example-project 'Create a new file called ErrorHandler.kt with basic error handling utilities' --interactive"
```

### Expected Output:

```
======================================================================
⚠️  FILE OPERATION REQUIRES APPROVAL
======================================================================
📁 File: example-project/src/main/kotlin/sample/ErrorHandler.kt
🔧 Action: CREATE new file

📄 NEW FILE
──────────────────────────────────────────────────────────────────────
+ package sample
+
+ object ErrorHandler {
+     fun handleError(error: Exception): String {
+         return "Error: ${error.message}"
+     }
+
+     fun logError(error: Exception) {
+         println("Error occurred: ${error.message}")
+         error.printStackTrace()
+     }
+ }
──────────────────────────────────────────────────────────────────────
Total: 12 lines

Options:
  [y] Yes    - Approve this operation
  [n] No     - Reject this operation
  [a] Always - Approve all operations (brave mode)
  [d] Deny   - Reject all operations
  [v] View   - Show full diff again
  [?] Help   - Show help

Your choice: _
```

## 🚀 Demo 3: Multiple Operations with "Always"

```bash
./gradlew jvmRun --args="example-project 'Add logging to all Calculator methods' --interactive"
```

First prompt:
```
Your choice: a
⚡ Brave mode enabled - all operations will be auto-approved
✅ Operation approved
```

All subsequent operations will be auto-approved without prompting!

## 🚀 Demo 4: Help Command

```
Your choice: ?

──────────────────────────────────────────────────────────────────────
📖 HELP - Interactive Mode
──────────────────────────────────────────────────────────────────────

This agent is requesting permission to modify files.

Available options:

  y/yes    - Approve this single operation
  n/no     - Reject this single operation
  a/always - Switch to brave mode (auto-approve everything)
  d/deny   - Reject all remaining operations
  v/view   - Show full file diff
  ?/help   - Show this help message

Diff Legend:
  + Green  - Lines being added
  - Red    - Lines being removed
    White  - Context lines (unchanged)

──────────────────────────────────────────────────────────────────────

Your choice: _
```

## 🎨 Color Output Preview

In a real terminal with color support, you'll see:

- **Green** (`+ lines`) - Additions
- **Red** (`- lines`) - Deletions
- **Cyan** (`📊 stats`) - Statistics
- **Gray** (`──lines`) - Separators
- **White** (` lines`) - Context

## 📊 Comparison: Different Modes

### Brave Mode (no prompts):
```bash
./gradlew jvmRun --args="example-project 'Add comments' --brave"
```
Output:
```
[Brave Mode] Auto-approving all operations
✓ Successfully edited file: Calculator.kt
```

### Interactive Mode (with diff):
```bash
./gradlew jvmRun --args="example-project 'Add comments' --interactive"
```
Output:
```
[Interactive Mode] Will prompt for approval with diff preview

======================================================================
⚠️  FILE OPERATION REQUIRES APPROVAL
======================================================================
📁 File: Calculator.kt
🔧 Action: OVERWRITE existing file

📝 FILE CHANGES (showing diff)
...
Your choice: _
```

### Safe Mode (default, auto-approves):
```bash
./gradlew jvmRun --args="example-project 'Add comments'"
```
Output:
```
[Safe Mode] Using default safe confirmation handler
✓ Successfully edited file: Calculator.kt
```

## 🔧 Advanced Usage

### With User Tracking:
```bash
./gradlew jvmRun --args="example-project 'task' --interactive --user developer@example.com"
```

### Disable Colors:
Edit `Main.kt` line 119:
```kotlin
InteractiveConfirmationHandler(useColors = false)
```

## 🎯 Real-World Scenarios

### Scenario 1: Code Review Before Merge
```bash
# Review AI changes before committing
./gradlew jvmRun --args="my-project 'Refactor UserService' --interactive"
# Review each diff, approve good changes, reject bad ones
```

### Scenario 2: Learning Tool
```bash
# See what the AI does step-by-step
./gradlew jvmRun --args="tutorial-project 'Add validation' --interactive"
# Learn from the changes the AI suggests
```

### Scenario 3: Safety Check
```bash
# Make sure AI doesn't break production code
./gradlew jvmRun --args="production-app 'Fix bug' --interactive"
# Carefully review each change
```

## 📸 Screenshots

### Terminal with Colors
```
[Image would show terminal with ANSI colors]
- Green additions
- Red deletions
- Clean formatting
```

### Full Diff View
```
[Image would show scrollable full diff]
- All changes visible
- Line numbers aligned
- Context preserved
```

## 🐛 Troubleshooting

### No colors showing?
- Use iTerm2 or modern terminal
- Check TERM environment variable
- Or disable colors: `useColors = false`

### Prompt not appearing?
- Don't use `--brave` flag
- Check stdin is available
- Verify `--interactive` flag is set

### Agent stops responding?
- You must enter a choice
- Press Ctrl+C to cancel
- Check terminal input

## 🎓 Next Steps

1. Try the demos above
2. Read [INTERACTIVE_MODE.md](docs/INTERACTIVE_MODE.md) for details
3. Explore IDE integration possibilities
4. Customize confirmation rules

## 📚 Related Docs

- [Interactive Mode Documentation](docs/INTERACTIVE_MODE.md)
- [Main README](README.md)
- [Sub-Agents](docs/SUBAGENTS.md)
