package com.agents

/**
 * Platform-agnostic interface for file system operations
 */
interface FileSystemProvider {
    val canWrite: Boolean
    fun listDirectory(path: String): Result<List<String>>
    fun readFile(path: String): Result<String>
    fun writeFile(path: String, content: String): Result<Unit>
}

/**
 * Expect declaration for platform-specific FileSystemProvider implementations
 */
expect object FileSystem {
    val readOnly: FileSystemProvider
    val readWrite: FileSystemProvider
}
