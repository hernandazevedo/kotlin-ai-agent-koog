package com.agents.config

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp

/**
 * Interactive menu with arrow key navigation using JLine3
 */
class InteractiveMenu {
    private val terminal: Terminal? = try {
        TerminalBuilder.builder()
            .system(true)
            .build()
    } catch (e: Exception) {
        null // Fallback if terminal not available
    }

    enum class Choice {
        YES, NO, ALWAYS, DENY, VIEW, HELP
    }

    private val choices = listOf(
        Choice.YES to "Yes - Approve this operation",
        Choice.NO to "No - Reject this operation",
        Choice.ALWAYS to "Always - Approve all operations (brave mode)",
        Choice.DENY to "Deny - Reject all operations",
        Choice.VIEW to "View - Show full diff again",
        Choice.HELP to "Help - Show help"
    )

    /**
     * Show interactive menu with arrow key navigation
     * @return Selected choice or null if terminal not available
     */
    fun showMenu(): Choice? {
        if (terminal == null) return null

        var selectedIndex = 0

        terminal.enterRawMode()
        try {
            while (true) {
                // Clear screen and move to top
                terminal.puts(InfoCmp.Capability.clear_screen)
                terminal.flush()

                // Print header
                println("=".repeat(70))
                println("⚠️  FILE OPERATION REQUIRES APPROVAL")
                println("=".repeat(70))
                println()
                println("Use ↑/↓ arrow keys to navigate, Enter to select:")
                println()

                // Print menu options
                choices.forEachIndexed { index, (choice, description) ->
                    if (index == selectedIndex) {
                        // Highlighted option
                        print("\u001B[7m")  // Reverse video
                        print("➤ ")
                    } else {
                        print("  ")
                    }

                    when (choice) {
                        Choice.YES -> print("✅ ")
                        Choice.NO -> print("❌ ")
                        Choice.ALWAYS -> print("⚡ ")
                        Choice.DENY -> print("🛑 ")
                        Choice.VIEW -> print("📄 ")
                        Choice.HELP -> print("❓ ")
                    }

                    print(description)

                    if (index == selectedIndex) {
                        print("\u001B[0m")  // Reset
                    }
                    println()
                }

                terminal.flush()

                // Read input
                val key = terminal.reader().read()

                when (key) {
                    27 -> { // ESC sequence (arrow keys)
                        if (terminal.reader().peek(100) > 0) {
                            terminal.reader().read() // read '['
                            val arrow = terminal.reader().read()
                            when (arrow.toChar()) {
                                'A' -> { // Up arrow
                                    selectedIndex = (selectedIndex - 1 + choices.size) % choices.size
                                }
                                'B' -> { // Down arrow
                                    selectedIndex = (selectedIndex + 1) % choices.size
                                }
                            }
                        }
                    }
                    13, 10 -> { // Enter
                        return choices[selectedIndex].first
                    }
                    'q'.code, 'Q'.code -> { // Quick quit
                        return Choice.NO
                    }
                }
            }
        } finally {
            terminal.close()
        }
    }

    /**
     * Fallback text-based menu (no arrow keys)
     */
    fun showTextMenu(): Choice? {
        println()
        println("Options:")
        println("  [y] Yes    - Approve this operation")
        println("  [n] No     - Reject this operation")
        println("  [a] Always - Approve all operations (brave mode)")
        println("  [d] Deny   - Reject all operations")
        println("  [v] View   - Show full diff again")
        println("  [?] Help   - Show help")
        println()

        print("Your choice: ")
        System.out.flush()

        val input = (System.console()?.readLine() ?: readlnOrNull())?.trim()?.lowercase()

        return when (input) {
            "y", "yes" -> Choice.YES
            "n", "no" -> Choice.NO
            "a", "always" -> Choice.ALWAYS
            "d", "deny" -> Choice.DENY
            "v", "view" -> Choice.VIEW
            "?", "help" -> Choice.HELP
            else -> null
        }
    }
}