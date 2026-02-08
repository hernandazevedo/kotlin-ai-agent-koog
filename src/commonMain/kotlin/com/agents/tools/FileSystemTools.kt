package com.agents.tools

import ai.koog.agents.core.tools.SimpleTool
import com.agents.FileSystemProvider
import kotlinx.serialization.Serializable

/**
 * Tool to list all files and directories in a specified path
 */
class ListDirectoryTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<ListDirectoryTool.Args>(
    name = "list_directory",
    description = "Lists all files and directories in the specified path",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(val path: String)

    override suspend fun execute(args: Args): String {
        return fileSystem.listDirectory(args.path).fold(
            onSuccess = { files ->
                val output = files.joinToString("\n") { "- $it" }
                "Contents of ${args.path}:\n$output"
            },
            onFailure = { error ->
                "Error: Failed to list directory: ${error.message}"
            }
        )
    }
}

/**
 * Tool to read the contents of a file
 */
class ReadFileTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<ReadFileTool.Args>(
    name = "read_file",
    description = "Reads the contents of a file at the specified path",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(val path: String)

    override suspend fun execute(args: Args): String {
        return fileSystem.readFile(args.path).fold(
            onSuccess = { content ->
                "File content of ${args.path}:\n```\n$content\n```"
            },
            onFailure = { error ->
                "Error: Failed to read file: ${error.message}"
            }
        )
    }
}

/**
 * Tool to create a new file with specified content
 */
class CreateFileTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<CreateFileTool.Args>(
    name = "create_file",
    description = "Creates a new file with the specified content",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(
        val path: String,
        val content: String
    )

    override suspend fun execute(args: Args): String {
        if (!fileSystem.canWrite) {
            return "Error: Write operations are not permitted with this file system provider"
        }

        return fileSystem.writeFile(args.path, args.content).fold(
            onSuccess = {
                "Successfully created file: ${args.path}"
            },
            onFailure = { error ->
                "Error: Failed to create file: ${error.message}"
            }
        )
    }
}

/**
 * Tool to edit an existing file by replacing its content
 */
class EditFileTool(
    private val fileSystem: FileSystemProvider
) : SimpleTool<EditFileTool.Args>(
    name = "edit_file",
    description = "Edits a file by replacing its content with new content",
    argsSerializer = Args.serializer()
) {
    @Serializable
    data class Args(
        val path: String,
        val content: String
    )

    override suspend fun execute(args: Args): String {
        if (!fileSystem.canWrite) {
            return "Error: Write operations are not permitted with this file system provider"
        }

        return fileSystem.writeFile(args.path, args.content).fold(
            onSuccess = {
                "Successfully updated file: ${args.path}"
            },
            onFailure = { error ->
                "Error: Failed to edit file: ${error.message}"
            }
        )
    }
}
