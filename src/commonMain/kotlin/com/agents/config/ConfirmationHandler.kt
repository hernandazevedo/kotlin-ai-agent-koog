package com.agents.config

/**
 * Handler for confirming potentially dangerous operations
 * Based on JetBrains Koog best practices
 */
interface ConfirmationHandler {
    /**
     * Request confirmation for a file write operation
     * @return true if the operation is approved, false otherwise
     */
    suspend fun requestFileWriteConfirmation(path: String, overwrite: Boolean): FileWriteConfirmation
}

/**
 * Result of a file write confirmation request
 */
sealed class FileWriteConfirmation {
    data object Approved : FileWriteConfirmation()
    data object Rejected : FileWriteConfirmation()
    data class Error(val message: String) : FileWriteConfirmation()
}

/**
 * Brave mode implementation - automatically approves all operations
 * Useful for automated scenarios
 */
class BraveConfirmationHandler : ConfirmationHandler {
    override suspend fun requestFileWriteConfirmation(path: String, overwrite: Boolean): FileWriteConfirmation {
        return FileWriteConfirmation.Approved
    }
}

/**
 * Safe mode implementation - provides safety checks
 * Can be extended to prompt user in interactive scenarios
 */
class SafeConfirmationHandler : ConfirmationHandler {
    override suspend fun requestFileWriteConfirmation(path: String, overwrite: Boolean): FileWriteConfirmation {
        // For now, auto-approve but this can be extended with actual user prompts
        // In a real scenario, this would check against rules or prompt the user
        return FileWriteConfirmation.Approved
    }
}
