# Interactive Mode - Troubleshooting Guide

## Problem: "Infinite loop" or "No input available"

### 🐛 **Root Cause**

When running via `./gradlew jvmRun`, Gradle **does not connect stdin to the application by default**. This causes `readlnOrNull()` to return `null` repeatedly, creating an infinite loop.

### ✅ **Solution 1: Use the Wrapper Script (Recommended)**

We provide a convenient script that handles stdin correctly:

```bash
./run-interactive.sh example-project "Add error handling to Calculator"
```

**What it does:**
- Builds the project
- Installs the distribution
- Runs the binary directly (which has stdin connected)
- Provides nice colored output

### ✅ **Solution 2: Run the Binary Directly**

Build and install first:
```bash
./gradlew installDist
```

Then run the binary:
```bash
./build/install/kotlin-ai-agent-koog/bin/kotlin-ai-agent-koog example-project "Your task" --interactive
```

### ✅ **Solution 3: Use Gradle with stdin Connected**

```bash
./gradlew jvmRun --console=plain --args="example-project 'task' --interactive" < /dev/tty
```

**Note:** This may not work on all systems.

### ✅ **Solution 4: Use Brave Mode**

If you don't need interactive prompts, use brave mode:

```bash
./gradlew jvmRun --args="example-project 'task' --brave"
```

---

## EOF Handling Implementation

### **Detection Strategy**

The `InteractiveConfirmationHandler` now implements multiple layers of EOF detection:

#### **1. Pre-check stdin availability**

Before showing any prompt, we check if stdin is available:

```kotlin
private fun checkStdinAvailability(): Boolean {
    return try {
        System.`in`.available() >= 0
    } catch (e: Exception) {
        false
    }
}
```

If stdin is not available, the handler:
- Prints a helpful error message
- Shows suggestions for fixing it
- Rejects the operation (safe default)

#### **2. Consecutive null counter**

During prompting, we track consecutive null reads:

```kotlin
private var consecutiveNulls = 0
private val maxConsecutiveNulls = 3

if (input == null) {
    consecutiveNulls++
    if (consecutiveNulls >= maxConsecutiveNulls) {
        // Exit after 3 consecutive nulls
        return FileWriteConfirmation.Rejected
    }
    Thread.sleep(100) // Small delay
    continue
}
```

**Why this works:**
- Real user input might have a brief delay → we retry
- EOF returns null immediately → we detect it after 3 attempts (300ms)
- Prevents infinite loops

#### **3. Exception handling**

Any exception during read is caught and treated as stdin failure:

```kotlin
val input = try {
    readlnOrNull()?.trim()?.lowercase()
} catch (e: Exception) {
    println("❌ Error reading input: ${e.message}")
    return FileWriteConfirmation.Rejected
}
```

---

## Error Messages and What They Mean

### **"Interactive mode requires stdin, but it's not available"**

**Meaning:** stdin check failed before showing prompt

**Solution:**
- Use `./run-interactive.sh` script
- Or run binary directly
- Or use `--brave` mode

### **"No input available after 3 attempts (stdin closed or EOF)"**

**Meaning:** stdin was initially available but returned null 3 times in a row

**Solution:**
- stdin was closed mid-execution
- Use a more stable execution method
- Check your terminal/shell

### **"Error reading input: [exception]"**

**Meaning:** Unexpected exception while reading

**Solution:**
- Check terminal/console settings
- Try a different terminal emulator
- Report the issue with the exception details

---

## Platform-Specific Issues

### **macOS / Linux**

Generally works well with:
```bash
./run-interactive.sh example-project "task"
```

If using Gradle directly, try:
```bash
./gradlew jvmRun --console=plain --args="..." < /dev/tty
```

### **Windows**

Gradle stdin handling is more problematic on Windows.

**Best approach:**
```powershell
.\gradlew.bat installDist
.\build\install\kotlin-ai-agent-koog\bin\kotlin-ai-agent-koog.bat example-project "task" --interactive
```

### **IDEs (IntelliJ IDEA, VS Code)**

Running from IDE Run Configuration may not connect stdin properly.

**Solution:**
- Run from integrated terminal instead
- Or create a Run Configuration that uses the wrapper script
- Or use brave mode for IDE execution

---

## Testing stdin Connection

Quick test to verify stdin works:

```bash
echo "y" | ./build/install/kotlin-ai-agent-koog/bin/kotlin-ai-agent-koog example-project "Add test" --interactive
```

**Expected:** Operation should be approved automatically

**If it fails:** stdin is not properly connected

---

## Debugging Tips

### **Enable verbose logging**

Add debug prints to see what's happening:

1. Edit `InteractiveConfirmationHandler.jvm.kt`
2. Add before `readlnOrNull()`:
   ```kotlin
   println("[DEBUG] About to read input...")
   println("[DEBUG] stdin.available(): ${System.`in`.available()}")
   ```

### **Check stdin state**

Run this test program:

```kotlin
fun main() {
    println("stdin available: ${System.`in`.available()}")
    println("Enter something:")
    val input = readlnOrNull()
    println("Read: $input")
}
```

If `available()` returns -1 or throws exception → stdin not connected

---

## Summary

| Method | stdin Works? | Recommended? |
|--------|-------------|--------------|
| `./run-interactive.sh` | ✅ Yes | ⭐ Best |
| Direct binary | ✅ Yes | ⭐ Good |
| `./gradlew jvmRun` | ❌ No | ❌ Don't use |
| `./gradlew jvmRun < /dev/tty` | ⚠️ Maybe | ⚠️ Unreliable |
| IDE Run Config | ❌ Usually no | ❌ Use terminal |
| `--brave` mode | N/A | ✅ Workaround |

---

## Related Documentation

- [Interactive Mode Guide](INTERACTIVE_MODE.md)
- [Main README](../README.md)
