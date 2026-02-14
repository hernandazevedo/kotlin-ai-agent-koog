package com.agents.config

/**
 * Utility for generating and displaying diffs between content
 * Based on unified diff format similar to git diff
 */
object DiffViewer {

    /**
     * Generate a unified diff between old and new content
     * @param oldContent Original content (null if file doesn't exist)
     * @param newContent New proposed content
     * @param contextLines Number of context lines to show around changes (default: 3)
     * @return Formatted diff string
     */
    fun generateDiff(
        oldContent: String?,
        newContent: String?,
        contextLines: Int = 3
    ): String {
        if (oldContent == null && newContent == null) {
            return "No content to compare"
        }

        if (oldContent == null) {
            return generateNewFileDiff(newContent!!)
        }

        if (newContent == null) {
            return generateDeletedFileDiff(oldContent)
        }

        val oldLines = oldContent.lines()
        val newLines = newContent.lines()

        return generateUnifiedDiff(oldLines, newLines, contextLines)
    }

    /**
     * Generate diff for a new file creation
     */
    private fun generateNewFileDiff(content: String): String {
        val lines = content.lines()
        val preview = lines.take(10)
        val hasMore = lines.size > 10

        return buildString {
            appendLine("📄 NEW FILE")
            appendLine("─".repeat(70))
            preview.forEach { line ->
                appendLine("+ $line")
            }
            if (hasMore) {
                appendLine("... (${lines.size - 10} more lines)")
            }
            appendLine("─".repeat(70))
            appendLine("Total: ${lines.size} lines")
        }
    }

    /**
     * Generate diff for a file deletion
     */
    private fun generateDeletedFileDiff(content: String): String {
        val lines = content.lines()
        val preview = lines.take(10)
        val hasMore = lines.size > 10

        return buildString {
            appendLine("🗑️  DELETED FILE")
            appendLine("─".repeat(70))
            preview.forEach { line ->
                appendLine("- $line")
            }
            if (hasMore) {
                appendLine("... (${lines.size - 10} more lines)")
            }
            appendLine("─".repeat(70))
            appendLine("Total: ${lines.size} lines removed")
        }
    }

    /**
     * Generate unified diff format
     */
    private fun generateUnifiedDiff(
        oldLines: List<String>,
        newLines: List<String>,
        contextLines: Int
    ): String {
        val changes = computeChanges(oldLines, newLines)

        if (changes.isEmpty()) {
            return "✅ No changes detected"
        }

        return buildString {
            appendLine("📝 FILE CHANGES")
            appendLine("─".repeat(70))

            val stats = DiffStats(
                additions = changes.count { it is DiffLine.Added },
                deletions = changes.count { it is DiffLine.Removed },
                modifications = changes.count { it is DiffLine.Context &&
                    changes.any { c -> c is DiffLine.Added || c is DiffLine.Removed } }
            )

            // Show statistics
            appendLine("📊 +${stats.additions} -${stats.deletions} ~${stats.modifications}")
            appendLine("─".repeat(70))

            // Show diff with line numbers
            var oldLineNum = 1
            var newLineNum = 1

            for (change in changes) {
                when (change) {
                    is DiffLine.Context -> {
                        appendLine("  ${oldLineNum.toString().padStart(4)} | ${change.content}")
                        oldLineNum++
                        newLineNum++
                    }
                    is DiffLine.Removed -> {
                        appendLine("- ${oldLineNum.toString().padStart(4)} | ${change.content}")
                        oldLineNum++
                    }
                    is DiffLine.Added -> {
                        appendLine("+ ${newLineNum.toString().padStart(4)} | ${change.content}")
                        newLineNum++
                    }
                }
            }

            appendLine("─".repeat(70))
        }
    }

    /**
     * Compute line-by-line changes using simple LCS algorithm
     */
    private fun computeChanges(oldLines: List<String>, newLines: List<String>): List<DiffLine> {
        val result = mutableListOf<DiffLine>()

        // Simple line-based diff using LCS (Longest Common Subsequence)
        val lcs = longestCommonSubsequence(oldLines, newLines)

        var oldIdx = 0
        var newIdx = 0
        var lcsIdx = 0

        while (oldIdx < oldLines.size || newIdx < newLines.size) {
            if (lcsIdx < lcs.size) {
                val lcsLine = lcs[lcsIdx]

                // Add removed lines before this common line
                while (oldIdx < oldLines.size && oldLines[oldIdx] != lcsLine) {
                    result.add(DiffLine.Removed(oldLines[oldIdx]))
                    oldIdx++
                }

                // Add added lines before this common line
                while (newIdx < newLines.size && newLines[newIdx] != lcsLine) {
                    result.add(DiffLine.Added(newLines[newIdx]))
                    newIdx++
                }

                // Add the common line
                if (oldIdx < oldLines.size && newIdx < newLines.size) {
                    result.add(DiffLine.Context(lcsLine))
                    oldIdx++
                    newIdx++
                    lcsIdx++
                }
            } else {
                // No more common lines, add remaining as removed/added
                while (oldIdx < oldLines.size) {
                    result.add(DiffLine.Removed(oldLines[oldIdx]))
                    oldIdx++
                }
                while (newIdx < newLines.size) {
                    result.add(DiffLine.Added(newLines[newIdx]))
                    newIdx++
                }
            }
        }

        return result
    }

    /**
     * Compute Longest Common Subsequence for diff algorithm
     */
    private fun longestCommonSubsequence(a: List<String>, b: List<String>): List<String> {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }

        // Build LCS length table
        for (i in 1..m) {
            for (j in 1..n) {
                if (a[i - 1] == b[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        // Backtrack to find LCS
        val lcs = mutableListOf<String>()
        var i = m
        var j = n

        while (i > 0 && j > 0) {
            if (a[i - 1] == b[j - 1]) {
                lcs.add(0, a[i - 1])
                i--
                j--
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }

        return lcs
    }

    /**
     * Format diff output with colors for terminal (ANSI codes)
     */
    fun formatWithColors(diff: String, useColors: Boolean = true): String {
        if (!useColors) return diff

        return diff.lines().joinToString("\n") { line ->
            when {
                line.startsWith("+") -> "\u001B[32m$line\u001B[0m" // Green
                line.startsWith("-") -> "\u001B[31m$line\u001B[0m" // Red
                line.startsWith("📊") -> "\u001B[36m$line\u001B[0m" // Cyan
                line.startsWith("─") -> "\u001B[90m$line\u001B[0m" // Gray
                else -> line
            }
        }
    }
}

/**
 * Represents a line in the diff
 */
sealed class DiffLine {
    data class Context(val content: String) : DiffLine()
    data class Added(val content: String) : DiffLine()
    data class Removed(val content: String) : DiffLine()
}

/**
 * Statistics about the diff
 */
data class DiffStats(
    val additions: Int,
    val deletions: Int,
    val modifications: Int
)
