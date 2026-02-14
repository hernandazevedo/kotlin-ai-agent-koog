package com.agents.tools

import com.agents.config.BraveConfirmationHandler
import com.agents.config.FileWriteConfirmation
import com.agents.config.ConfirmationHandler
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Mock ConfirmationHandler for testing
 */
class MockShellConfirmationHandler(
    private val shouldApprove: Boolean = true
) : ConfirmationHandler {
    override suspend fun requestFileWriteConfirmation(
        path: String,
        overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        return if (shouldApprove) FileWriteConfirmation.Approved else FileWriteConfirmation.Rejected
    }
}

class ShellCommandToolTest {

    @Test
    fun testExecuteShellCommand_simpleEcho() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = "echo 'Hello from shell'",
                timeoutSeconds = 5
            )
        )

        assertContains(result, "Hello from shell")
        assertContains(result, "Exit code: 0")
    }

    @Test
    fun testExecuteShellCommand_listFiles() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        // Use a command that works on both Unix and Windows
        val os = System.getProperty("os.name").lowercase()
        val command = if (os.contains("win")) "dir" else "ls"

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = command,
                timeoutSeconds = 5
            )
        )

        assertContains(result, "Command executed")
        assertContains(result, "Exit code: 0")
    }

    @Test
    fun testExecuteShellCommand_emptyCommand() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = "",
                timeoutSeconds = 5
            )
        )

        assertContains(result, "Error")
        assertContains(result, "Command cannot be empty")
    }

    @Test
    fun testExecuteShellCommand_invalidTimeout() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = "echo test",
                timeoutSeconds = 0
            )
        )

        assertContains(result, "Error")
        assertContains(result, "Timeout must be positive")
    }

    @Test
    fun testExecuteShellCommand_rejected() = runBlocking {
        val tool = ExecuteShellCommandTool(MockShellConfirmationHandler(shouldApprove = false))

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = "echo test",
                timeoutSeconds = 5
            )
        )

        assertContains(result, "rejected")
        assertContains(result, "not approved")
    }

    @Test
    fun testExecuteShellCommand_withWorkingDirectory() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val os = System.getProperty("os.name").lowercase()
        val command = if (os.contains("win")) "cd" else "pwd"

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = command,
                timeoutSeconds = 5,
                workingDirectory = System.getProperty("user.home")
            )
        )

        assertContains(result, "Command executed")
    }

    @Test
    fun testExecuteShellCommand_commandFails() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        // This command should fail with non-zero exit code
        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = "exit 1",
                timeoutSeconds = 5
            )
        )

        assertContains(result, "Command executed")
        assertContains(result, "Exit code: 1")
    }

    @Test
    fun testExecuteShellCommand_timeout() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val os = System.getProperty("os.name").lowercase()
        // Use a command that takes longer than timeout
        val command = if (os.contains("win")) {
            "timeout /t 10"
        } else {
            "sleep 10"
        }

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = command,
                timeoutSeconds = 1
            )
        )

        assertContains(result, "timed out", ignoreCase = true)
    }

    @Test
    fun testExecuteShellCommand_multilineOutput() = runBlocking {
        val tool = ExecuteShellCommandTool(BraveConfirmationHandler())

        val os = System.getProperty("os.name").lowercase()
        val command = if (os.contains("win")) {
            "echo Line1 && echo Line2 && echo Line3"
        } else {
            "echo 'Line1' && echo 'Line2' && echo 'Line3'"
        }

        val result = tool.execute(
            ExecuteShellCommandTool.Args(
                command = command,
                timeoutSeconds = 5
            )
        )

        assertContains(result, "Line1")
        assertContains(result, "Line2")
        assertContains(result, "Line3")
    }

    @Test
    fun testShellCommandResult_structure() {
        val result = ShellCommandResult(
            command = "echo test",
            exitCode = 0,
            output = "test",
            timedOut = false
        )

        assertTrue(result.command == "echo test")
        assertTrue(result.exitCode == 0)
        assertTrue(result.output == "test")
        assertTrue(!result.timedOut)
    }

    @Test
    fun testShellCommandResult_timeout() {
        val result = ShellCommandResult(
            command = "sleep 10",
            exitCode = null,
            output = "partial output",
            timedOut = true
        )

        assertTrue(result.exitCode == null)
        assertTrue(result.timedOut)
        assertTrue(result.output == "partial output")
    }
}
