package com.agents

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * JVM-specific FileSystemProvider implementation
 */
private class JVMFileSystemProvider(override val canWrite: Boolean) : FileSystemProvider {

    override fun listDirectory(path: String): Result<List<String>> = runCatching {
        val dir = File(path)
        if (!dir.exists()) {
            return Result.failure(Exception("Directory does not exist: $path"))
        }
        if (!dir.isDirectory) {
            return Result.failure(Exception("Path is not a directory: $path"))
        }
        dir.listFiles()?.map { it.name } ?: emptyList()
    }

    override fun readFile(path: String): Result<String> = runCatching {
        val file = Path.of(path)
        if (!file.exists()) {
            return Result.failure(Exception("File does not exist: $path"))
        }
        file.readText()
    }

    override fun writeFile(path: String, content: String): Result<Unit> = runCatching {
        if (!canWrite) {
            return Result.failure(Exception("Write operation not permitted"))
        }
        val file = File(path)
        file.parentFile?.mkdirs() // Create parent directories if needed
        file.writeText(content)
    }
}

/**
 * Actual implementation for JVM platform
 */
actual object FileSystem {
    actual val readOnly: FileSystemProvider = JVMFileSystemProvider(canWrite = false)
    actual val readWrite: FileSystemProvider = JVMFileSystemProvider(canWrite = true)
}
