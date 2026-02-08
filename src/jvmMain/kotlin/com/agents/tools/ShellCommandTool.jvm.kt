package com.agents.tools

import ai.koog.agents.core.tools.SimpleTool
import com.agents.config.ConfirmationHandler
import com.agents.config.FileWriteConfirmation
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Result of a shell command execution
 * Following JetBrains Koog best practices with structured Result class
 */
@Serializable
data class ShellCommandResult(
    val command: String,
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean = false
)

/**
 * JVM implementation of ExecuteShellCommandTool
 * Following the pattern from JetBrains Koog article with timeout handling
 */
class ExecuteShellCommandTool(
    private val confirmationHandler: ConfirmationHandler
) : SimpleTool<ExecuteShellCommandTool.Args>(
    name = "execute__shell_command",
    description = """
        Executes a shell command with timeout support.
        Use this to run system commands, build tools (gradle, npm), or scripts.
        The command will be terminated if it exceeds the timeout.
        Partial output is preserved even if the command times out.
    """.trimIndent(),
    argsSerializer = Args.serializer()
) {

    @Serializable
    data class Args(
        val command: String,
        val timeoutSeconds: Long = 30,
        val workingDirectory: String? = null
    )

    override suspend fun execute(args: Args): String {
        // Validate command
        if (args.command.isBlank()) {
            return buildErrorResult("Command cannot be empty")
        }

        // Validate timeout
        if (args.timeoutSeconds <= 0) {
            return buildErrorResult("Timeout must be positive")
        }

        // Request confirmation (treating shell commands as potentially dangerous operations)
        return when (val confirmation = confirmationHandler.requestFileWriteConfirmation(
            path = "shell_command: ${args.command}",
            overwrite = false
        )) {
            is FileWriteConfirmation.Approved -> {
                executeCommand(args)
            }
            is FileWriteConfirmation.Rejected -> {
                buildDeniedResult(args.command)
            }
            is FileWriteConfirmation.Error -> {
                buildErrorResult("Confirmation error: ${confirmation.message}")
            }
        }
    }

    private suspend fun executeCommand(args: Args): String {
        val result = try {
            executeWithTimeout(args)
        } catch (e: Exception) {
            return buildErrorResult("Failed to execute command: ${e.message}")
        }

        return formatResult(result)
    }

    /**
     * Execute command with timeout handling and partial output preservation
     * Based on the pattern from JetBrains Koog article
     */
    private suspend fun executeWithTimeout(args: Args): ShellCommandResult = coroutineScope {
        val processBuilder = ProcessBuilder()
            .command(getShellCommand(args.command))
            .redirectErrorStream(true) // Merge stderr into stdout

        // Set working directory if specified
        args.workingDirectory?.let {
            val workDir = File(it)
            if (workDir.exists() && workDir.isDirectory) {
                processBuilder.directory(workDir)
            }
        }

        val process = processBuilder.start()
        val outputBuilder = StringBuilder()

        // Read output in a separate coroutine
        val outputJob = launch(Dispatchers.IO) {
            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    outputBuilder.appendLine(line)
                }
            }
        }

        // Wait for process with timeout
        val isCompleted = withTimeoutOrNull(args.timeoutSeconds * 1000L) {
            withContext(Dispatchers.IO) {
                process.waitFor()
            }
            outputJob.join()
            true
        } != null

        val exitCode: Int?
        val timedOut: Boolean

        if (!isCompleted) {
            // Timeout occurred - forcibly terminate and preserve partial output
            process.destroyForcibly()
            outputJob.cancel()
            exitCode = null
            timedOut = true
        } else {
            exitCode = process.exitValue()
            timedOut = false
        }

        ShellCommandResult(
            command = args.command,
            exitCode = exitCode,
            output = outputBuilder.toString().trim(),
            timedOut = timedOut
        )
    }

    /**
     * Get platform-specific shell command wrapper
     */
    private fun getShellCommand(command: String): List<String> {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("win")) {
            listOf("cmd.exe", "/c", command)
        } else {
            listOf("/bin/sh", "-c", command)
        }
    }

    private fun formatResult(result: ShellCommandResult): String {
        return buildString {
            appendLine("✓ Command executed: ${result.command}")

            if (result.timedOut) {
                appendLine("⚠️  Command timed out (partial output preserved)")
            } else {
                appendLine("Exit code: ${result.exitCode ?: "N/A"}")
            }

            if (result.output.isNotEmpty()) {
                appendLine("\nOutput:")
                appendLine("```")
                appendLine(result.output)
                appendLine("```")
            } else {
                appendLine("\n(No output)")
            }
        }
    }

    private fun buildDeniedResult(command: String): String {
        return "Operation rejected: Shell command execution was not approved\nCommand: $command"
    }

    private fun buildErrorResult(message: String): String {
        return """
            Error: $message

            Suggestion:
            - Verify the command syntax is correct
            - Ensure required tools/binaries are installed
            - Check working directory exists (if specified)
            - Consider increasing timeout for long-running commands
        """.trimIndent()
    }
}
