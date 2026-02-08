package com.agents.tools

import ai.koog.agents.core.tools.SimpleTool
import com.agents.FileSystemProvider
import com.agents.config.ConfirmationHandler
import com.agents.config.FileWriteConfirmation
import com.agents.validation.ToolValidation
import kotlinx.serialization.Serializable

/**
 * Tool to list all files and directories in a specified path
 * Follows JetBrains Koog best practices with proper validation and error handling
 */
class ListDirectoryTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<ListDirectoryTool.Args>(
    name = "list__directory",
    description = "Lists all files and directories in the specified path. Returns a list of file and directory names.",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(val path: String)

    override suspend fun execute(args: Args): String {
        // Validate input
        val validation = ToolValidation.validatePath(args.path)
        if (!validation.isValid) {
            return buildErrorResult("Validation failed: ${validation.errorMessage}")
        }

        return fileSystem.listDirectory(args.path).fold(
            onSuccess = { files ->
                buildSuccessResult(args.path, files)
            },
            onFailure = { error ->
                buildErrorResult("Failed to list directory '${args.path}': ${error.message ?: "Unknown error"}")
            }
        )
    }

    private fun buildSuccessResult(path: String, files: List<String>): String {
        return if (files.isEmpty()) {
            "Directory '$path' is empty"
        } else {
            val output = files.joinToString("\n") { "- $it" }
            "Contents of '$path' (${files.size} items):\n$output"
        }
    }

    private fun buildErrorResult(message: String): String {
        return "Error: $message\n\nSuggestion: Verify the path exists and you have permission to access it."
    }
}

/**
 * Tool to read the contents of a file
 * Follows JetBrains Koog best practices with proper validation and error handling
 */
class ReadFileTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<ReadFileTool.Args>(
    name = "read__file",
    description = "Reads the contents of a file at the specified path. Returns the complete file content.",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(val path: String)

    override suspend fun execute(args: Args): String {
        // Validate input
        val validation = ToolValidation.validatePath(args.path)
        if (!validation.isValid) {
            return buildErrorResult("Validation failed: ${validation.errorMessage}")
        }

        return fileSystem.readFile(args.path).fold(
            onSuccess = { content ->
                buildSuccessResult(args.path, content)
            },
            onFailure = { error ->
                buildErrorResult("Failed to read file '${args.path}': ${error.message ?: "Unknown error"}")
            }
        )
    }

    private fun buildSuccessResult(path: String, content: String): String {
        val lineCount = content.lines().size
        val charCount = content.length
        return """
            File: $path
            Lines: $lineCount | Characters: $charCount

            ```
            $content
            ```
        """.trimIndent()
    }

    private fun buildErrorResult(message: String): String {
        return "Error: $message\n\nSuggestion: Ensure the file exists and is readable. Use list_directory to verify the file path."
    }
}

/**
 * Tool to create a new file with specified content
 * Follows JetBrains Koog best practices with validation, confirmation, and error handling
 */
class CreateFileTool(
    private val fileSystem: FileSystemProvider,
    private val confirmationHandler: ConfirmationHandler
) : SimpleTool<CreateFileTool.Args>(
    name = "create__file",
    description = "Creates a new file with the specified content. Use this only for new files, not for editing existing ones.",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(
        val path: String,
        val content: String
    )

    override suspend fun execute(args: Args): String {
        // Check write permissions
        if (!fileSystem.canWrite) {
            return buildErrorResult("Write operations are not permitted with this file system provider")
        }

        // Validate path
        val pathValidation = ToolValidation.validatePath(args.path)
        if (!pathValidation.isValid) {
            return buildErrorResult("Path validation failed: ${pathValidation.errorMessage}")
        }

        // Validate content
        val contentValidation = ToolValidation.validateContent(args.content)
        if (!contentValidation.isValid) {
            return buildErrorResult("Content validation failed: ${contentValidation.errorMessage}")
        }

        // Request confirmation
        return when (val confirmation = confirmationHandler.requestFileWriteConfirmation(args.path, overwrite = false)) {
            is FileWriteConfirmation.Approved -> {
                performFileWrite(args)
            }
            is FileWriteConfirmation.Rejected -> {
                "Operation rejected: File creation was not approved"
            }
            is FileWriteConfirmation.Error -> {
                buildErrorResult("Confirmation error: ${confirmation.message}")
            }
        }
    }

    private suspend fun performFileWrite(args: Args): String {
        return try {
            fileSystem.writeFile(args.path, args.content).fold(
                onSuccess = {
                    buildSuccessResult(args.path, args.content)
                },
                onFailure = { error ->
                    buildErrorResult("Failed to create file '${args.path}': ${error.message ?: "Unknown error"}")
                }
            )
        } catch (e: Exception) {
            buildErrorResult("Exception during file creation: ${e.message}")
        }
    }

    private fun buildSuccessResult(path: String, content: String): String {
        val lines = content.lines().size
        val chars = content.length
        return "✓ Successfully created file: $path\n  Lines: $lines | Characters: $chars"
    }

    private fun buildErrorResult(message: String): String {
        return "Error: $message\n\nSuggestion: Verify the directory exists and you have write permissions."
    }
}

/**
 * Tool to edit an existing file by replacing its content
 * Follows JetBrains Koog best practices with validation, confirmation, and error handling
 */
class EditFileTool(
    private val fileSystem: FileSystemProvider,
    private val confirmationHandler: ConfirmationHandler
) : SimpleTool<EditFileTool.Args>(
    name = "edit__file",
    description = "Edits a file by replacing its content with new content. Use this to modify existing files.",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(
        val path: String,
        val content: String
    )

    override suspend fun execute(args: Args): String {
        // Check write permissions
        if (!fileSystem.canWrite) {
            return buildErrorResult("Write operations are not permitted with this file system provider")
        }

        // Validate path
        val pathValidation = ToolValidation.validatePath(args.path)
        if (!pathValidation.isValid) {
            return buildErrorResult("Path validation failed: ${pathValidation.errorMessage}")
        }

        // Validate content
        val contentValidation = ToolValidation.validateContent(args.content)
        if (!contentValidation.isValid) {
            return buildErrorResult("Content validation failed: ${contentValidation.errorMessage}")
        }

        // Request confirmation (this is an overwrite operation)
        return when (val confirmation = confirmationHandler.requestFileWriteConfirmation(args.path, overwrite = true)) {
            is FileWriteConfirmation.Approved -> {
                performFileEdit(args)
            }
            is FileWriteConfirmation.Rejected -> {
                "Operation rejected: File edit was not approved"
            }
            is FileWriteConfirmation.Error -> {
                buildErrorResult("Confirmation error: ${confirmation.message}")
            }
        }
    }

    private suspend fun performFileEdit(args: Args): String {
        return try {
            fileSystem.writeFile(args.path, args.content).fold(
                onSuccess = {
                    buildSuccessResult(args.path, args.content)
                },
                onFailure = { error ->
                    buildErrorResult("Failed to edit file '${args.path}': ${error.message ?: "Unknown error"}")
                }
            )
        } catch (e: Exception) {
            buildErrorResult("Exception during file edit: ${e.message}")
        }
    }

    private fun buildSuccessResult(path: String, content: String): String {
        val lines = content.lines().size
        val chars = content.length
        return "✓ Successfully updated file: $path\n  Lines: $lines | Characters: $chars"
    }

    private fun buildErrorResult(message: String): String {
        return "Error: $message\n\nSuggestion: Verify the file exists and you have write permissions. Use read_file to check the current content first."
    }
}
