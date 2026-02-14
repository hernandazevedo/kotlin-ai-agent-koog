package com.agents.config

/**
 * Handler for confirming potentially dangerous operations
 * Based on JetBrains Koog best practices
 */
interface ConfirmationHandler {
    /**
     * Request confirmation for a file write operation
     * @param path The file path
     * @param overwrite Whether this operation will overwrite an existing file
     * @param oldContent The current content of the file (if it exists)
     * @param newContent The proposed new content
     * @return FileWriteConfirmation result
     */
    suspend fun requestFileWriteConfirmation(
        path: String,
        overwrite: Boolean,
        oldContent: String? = null,
        newContent: String? = null
    ): FileWriteConfirmation
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
    override suspend fun requestFileWriteConfirmation(
        path: String,
        overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        return FileWriteConfirmation.Approved
    }
}

/**
 * Safe mode implementation - provides safety checks
 * Can be extended to prompt user in interactive scenarios
 */
class SafeConfirmationHandler : ConfirmationHandler {
    override suspend fun requestFileWriteConfirmation(
        path: String,
        overwrite: Boolean,
        oldContent: String?,
        newContent: String?
    ): FileWriteConfirmation {
        // For now, auto-approve but this can be extended with actual user prompts
        // In a real scenario, this would check against rules or prompt the user
        return FileWriteConfirmation.Approved
    }
}
