package com.agents.config

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ConfirmationHandlerTest {

    @Test
    fun testBraveConfirmationHandler_alwaysApproves() = runTest {
        val handler = BraveConfirmationHandler()

        val result = handler.requestFileWriteConfirmation(
            path = "/test/file.txt",
            overwrite = false
        )

        assertTrue(result is FileWriteConfirmation.Approved, "BraveConfirmationHandler should always approve")
    }

    @Test
    fun testBraveConfirmationHandler_approvesOverwrite() = runTest {
        val handler = BraveConfirmationHandler()

        val result = handler.requestFileWriteConfirmation(
            path = "/test/file.txt",
            overwrite = true
        )

        assertTrue(result is FileWriteConfirmation.Approved, "BraveConfirmationHandler should approve overwrites")
    }

    @Test
    fun testSafeConfirmationHandler_approves() = runTest {
        val handler = SafeConfirmationHandler()

        val result = handler.requestFileWriteConfirmation(
            path = "/test/file.txt",
            overwrite = false
        )

        // Current implementation auto-approves
        // TODO: Update when interactive mode is implemented
        assertTrue(result is FileWriteConfirmation.Approved, "SafeConfirmationHandler currently auto-approves")
    }

    @Test
    fun testSafeConfirmationHandler_approvesOverwrite() = runTest {
        val handler = SafeConfirmationHandler()

        val result = handler.requestFileWriteConfirmation(
            path = "/test/file.txt",
            overwrite = true
        )

        // Current implementation auto-approves
        // TODO: Update when interactive mode is implemented
        assertTrue(result is FileWriteConfirmation.Approved, "SafeConfirmationHandler currently auto-approves")
    }
}
