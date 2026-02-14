package com.agents.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interactive confirmation handler that prompts the user via console
 * Shows diff preview before file operations
 */
class InteractiveConfirmationHandler(
    private val useColors: Boolean = true
) : ConfirmationHandler {

    private var alwaysApprove = false
    private var alwaysDeny = false
    private var stdinAvailable: Boolean? = null // Cache stdin availability check
    private var consecutiveNulls = 0 // Track consecutive null reads
    private val maxConsecutiveNulls = 3 // Max attempts before giving up

    override suspend fun requestFileWriteConfirmation(
        path: String,
        overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        // If user already chose "always approve/deny"
        if (alwaysApprove) return FileWriteConfirmation.Approved
        if (alwaysDeny) return FileWriteConfirmation.Rejected

        // Check if stdin is available (only once)
        if (stdinAvailable == null) {
            stdinAvailable = checkStdinAvailability()
        }

        if (stdinAvailable == false) {
            println()
            println("⚠️  Interactive mode requires stdin, but it's not available")
            println("💡 Suggestions:")
            println("   1. Use --brave flag to auto-approve all operations")
            println("   2. Run directly: ./build/install/kotlin-ai-agent-koog/bin/kotlin-ai-agent-koog")
            println("   3. Use: ./gradlew jvmRun --console=plain --args=\"...\" < /dev/tty")
            println()
            println("⚠️  Defaulting to REJECT for safety")
            return FileWriteConfirmation.Rejected
        }

        return withContext(Dispatchers.IO) {
            showConfirmationPrompt(path, overwrite, oldContent, newContent)
        }
    }

    private fun checkStdinAvailability(): Boolean {
        return try {
            // Check if System.in is connected and available
            System.`in`.available() >= 0
        } catch (e: Exception) {
            false
        }
    }

    private fun showConfirmationPrompt(
        path: String,
        overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        println()
        println("=".repeat(70))
        println("⚠️  FILE OPERATION REQUIRES APPROVAL")
        println("=".repeat(70))
        println("📁 File: $path")
        println("🔧 Action: ${if (overwrite) "OVERWRITE existing file" else "CREATE new file"}")
        println()

        // Show diff preview
        if (oldContent != null || newContent != null) {
            val diff = DiffViewer.generateDiff(oldContent, newContent)
            val formattedDiff = DiffViewer.formatWithColors(diff, useColors)
            println(formattedDiff)
            println()
        }

        println("Options:")
        println("  [y] Yes    - Approve this operation")
        println("  [n] No     - Reject this operation")
        println("  [a] Always - Approve all operations (brave mode)")
        println("  [d] Deny   - Reject all operations")
        println("  [v] View   - Show full diff again")
        println("  [?] Help   - Show help")
        println()

        return promptUser(path, overwrite, oldContent, newContent)
    }

    private fun promptUser(
        @Suppress("UNUSED_PARAMETER") path: String,
        @Suppress("UNUSED_PARAMETER") overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        while (true) {
            print("Your choice: ")
            System.out.flush() // Force flush to ensure prompt is shown

            val input = try {
                // Use System.console() for better Gradle compatibility
                // Falls back to readlnOrNull() if console is not available
                (System.console()?.readLine() ?: readlnOrNull())?.trim()?.lowercase()
            } catch (e: Exception) {
                println("\n❌ Error reading input: ${e.message}")
                println("⚠️  Defaulting to REJECT for safety")
                return FileWriteConfirmation.Rejected
            }

            if (input == null) {
                consecutiveNulls++
                if (consecutiveNulls >= maxConsecutiveNulls) {
                    println("\n⚠️  No input available after $maxConsecutiveNulls attempts (stdin closed or EOF)")
                    println("⚠️  Interactive mode cannot continue - defaulting to REJECT for safety")
                    println("💡 Tip: Use --brave flag or run outside Gradle")
                    return FileWriteConfirmation.Rejected
                }
                // Give user a moment to provide input
                Thread.sleep(100)
                continue
            }

            // Reset counter on successful read
            consecutiveNulls = 0

            when (input) {
                "y", "yes" -> {
                    println("✅ Operation approved")
                    return FileWriteConfirmation.Approved
                }
                "n", "no" -> {
                    println("❌ Operation rejected")
                    return FileWriteConfirmation.Rejected
                }
                "a", "always" -> {
                    println("⚡ Brave mode enabled - all operations will be auto-approved")
                    alwaysApprove = true
                    return FileWriteConfirmation.Approved
                }
                "d", "deny" -> {
                    println("🛑 All operations will be rejected")
                    alwaysDeny = true
                    return FileWriteConfirmation.Rejected
                }
                "v", "view" -> {
                    showFullDiff(oldContent, newContent)
                    println() // Add spacing after diff
                }
                "?", "help" -> {
                    showHelp()
                }
                "" -> {
                    println("⚠️  Please enter a choice (y/n/a/d/v/?)")
                }
                else -> {
                    println("❌ Invalid option: '$input'. Please choose y/n/a/d/v/?")
                }
            }
        }
    }

    private fun showFullDiff(oldContent: String?, newContent: String?) {
        println()
        println("─".repeat(70))
        println("📄 FULL DIFF VIEW")
        println("─".repeat(70))

        val diff = DiffViewer.generateDiff(oldContent, newContent, contextLines = 1000)
        val formattedDiff = DiffViewer.formatWithColors(diff, useColors)
        println(formattedDiff)

        println("─".repeat(70))
        println()
    }

    private fun showHelp() {
        println()
        println("─".repeat(70))
        println("📖 HELP - Interactive Mode")
        println("─".repeat(70))
        println()
        println("This agent is requesting permission to modify files.")
        println()
        println("Available options:")
        println()
        println("  y/yes    - Approve this single operation")
        println("  n/no     - Reject this single operation")
        println("  a/always - Switch to brave mode (auto-approve everything)")
        println("  d/deny   - Reject all remaining operations")
        println("  v/view   - Show full file diff")
        println("  ?/help   - Show this help message")
        println()
        println("Diff Legend:")
        println("  + Green  - Lines being added")
        println("  - Red    - Lines being removed")
        println("    White  - Context lines (unchanged)")
        println()
        println("─".repeat(70))
        println()
    }
}
