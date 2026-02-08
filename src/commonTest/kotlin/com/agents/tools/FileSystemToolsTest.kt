package com.agents.tools

import com.agents.FileSystemProvider
import com.agents.config.BraveConfirmationHandler
import com.agents.config.FileWriteConfirmation
import com.agents.config.ConfirmationHandler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Mock FileSystemProvider for testing
 */
class MockFileSystemProvider(
    override val canWrite: Boolean = true,
    private val files: MutableMap<String, String> = mutableMapOf(),
    private val directories: MutableMap<String, List<String>> = mutableMapOf()
) : FileSystemProvider {

    override fun listDirectory(path: String): Result<List<String>> {
        return directories[path]?.let { Result.success(it) }
            ?: Result.failure(Exception("Directory not found: $path"))
    }

    override fun readFile(path: String): Result<String> {
        return files[path]?.let { Result.success(it) }
            ?: Result.failure(Exception("File not found: $path"))
    }

    override fun writeFile(path: String, content: String): Result<Unit> {
        return if (canWrite) {
            files[path] = content
            Result.success(Unit)
        } else {
            Result.failure(Exception("Write not permitted"))
        }
    }
}

/**
 * Mock ConfirmationHandler that can be configured to approve or reject
 */
class MockConfirmationHandler(
    private val shouldApprove: Boolean = true
) : ConfirmationHandler {
    override suspend fun requestFileWriteConfirmation(path: String, overwrite: Boolean): FileWriteConfirmation {
        return if (shouldApprove) FileWriteConfirmation.Approved else FileWriteConfirmation.Rejected
    }
}

class FileSystemToolsTest {

    @Test
    fun testListDirectoryTool_success() = runTest {
        val mockFs = MockFileSystemProvider(
            directories = mutableMapOf(
                "/test" to listOf("file1.txt", "file2.txt", "dir1")
            )
        )
        val tool = ListDirectoryTool(mockFs)

        val result = tool.execute(ListDirectoryTool.Args("/test"))

        assertContains(result, "file1.txt")
        assertContains(result, "file2.txt")
        assertContains(result, "dir1")
        assertContains(result, "3 items")
    }

    @Test
    fun testListDirectoryTool_emptyDirectory() = runTest {
        val mockFs = MockFileSystemProvider(
            directories = mutableMapOf("/empty" to emptyList())
        )
        val tool = ListDirectoryTool(mockFs)

        val result = tool.execute(ListDirectoryTool.Args("/empty"))

        assertContains(result, "empty")
    }

    @Test
    fun testListDirectoryTool_invalidPath() = runTest {
        val mockFs = MockFileSystemProvider()
        val tool = ListDirectoryTool(mockFs)

        val result = tool.execute(ListDirectoryTool.Args(""))

        assertContains(result, "Error")
        assertContains(result, "Path cannot be empty")
    }

    @Test
    fun testListDirectoryTool_pathTraversal() = runTest {
        val mockFs = MockFileSystemProvider()
        val tool = ListDirectoryTool(mockFs)

        val result = tool.execute(ListDirectoryTool.Args("/test/../etc"))

        assertContains(result, "Error")
        assertContains(result, "security reasons")
    }

    @Test
    fun testReadFileTool_success() = runTest {
        val mockFs = MockFileSystemProvider(
            files = mutableMapOf("/test/file.txt" to "Hello, World!")
        )
        val tool = ReadFileTool(mockFs)

        val result = tool.execute(ReadFileTool.Args("/test/file.txt"))

        assertContains(result, "Hello, World!")
        assertContains(result, "Lines: 1")
        assertContains(result, "Characters: 13")
    }

    @Test
    fun testReadFileTool_multilineFile() = runTest {
        val content = "Line 1\nLine 2\nLine 3"
        val mockFs = MockFileSystemProvider(
            files = mutableMapOf("/test/file.txt" to content)
        )
        val tool = ReadFileTool(mockFs)

        val result = tool.execute(ReadFileTool.Args("/test/file.txt"))

        assertContains(result, content)
        assertContains(result, "Lines: 3")
    }

    @Test
    fun testReadFileTool_fileNotFound() = runTest {
        val mockFs = MockFileSystemProvider()
        val tool = ReadFileTool(mockFs)

        val result = tool.execute(ReadFileTool.Args("/nonexistent.txt"))

        assertContains(result, "Error")
        assertContains(result, "File not found")
    }

    @Test
    fun testCreateFileTool_success() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = BraveConfirmationHandler()
        val tool = CreateFileTool(mockFs, confirmationHandler)

        val result = tool.execute(CreateFileTool.Args("/test/new.txt", "New content"))

        assertContains(result, "Successfully created")
        assertContains(result, "/test/new.txt")
        assertContains(result, "Lines: 1")
    }

    @Test
    fun testCreateFileTool_rejected() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = MockConfirmationHandler(shouldApprove = false)
        val tool = CreateFileTool(mockFs, confirmationHandler)

        val result = tool.execute(CreateFileTool.Args("/test/new.txt", "Content"))

        assertContains(result, "rejected")
        assertContains(result, "not approved")
    }

    @Test
    fun testCreateFileTool_invalidPath() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = BraveConfirmationHandler()
        val tool = CreateFileTool(mockFs, confirmationHandler)

        val result = tool.execute(CreateFileTool.Args("", "Content"))

        assertContains(result, "Error")
        assertContains(result, "Path cannot be empty")
    }

    @Test
    fun testCreateFileTool_emptyContent() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = BraveConfirmationHandler()
        val tool = CreateFileTool(mockFs, confirmationHandler)

        val result = tool.execute(CreateFileTool.Args("/test/file.txt", ""))

        assertContains(result, "Error")
        assertContains(result, "Content cannot be empty")
    }

    @Test
    fun testCreateFileTool_readOnlyFileSystem() = runTest {
        val mockFs = MockFileSystemProvider(canWrite = false)
        val confirmationHandler = BraveConfirmationHandler()
        val tool = CreateFileTool(mockFs, confirmationHandler)

        val result = tool.execute(CreateFileTool.Args("/test/file.txt", "Content"))

        assertContains(result, "Error")
        assertContains(result, "not permitted")
    }

    @Test
    fun testEditFileTool_success() = runTest {
        val mockFs = MockFileSystemProvider(
            files = mutableMapOf("/test/file.txt" to "Old content")
        )
        val confirmationHandler = BraveConfirmationHandler()
        val tool = EditFileTool(mockFs, confirmationHandler)

        val result = tool.execute(EditFileTool.Args("/test/file.txt", "New content"))

        assertContains(result, "Successfully updated")
        assertContains(result, "/test/file.txt")
    }

    @Test
    fun testEditFileTool_rejected() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = MockConfirmationHandler(shouldApprove = false)
        val tool = EditFileTool(mockFs, confirmationHandler)

        val result = tool.execute(EditFileTool.Args("/test/file.txt", "New content"))

        assertContains(result, "rejected")
        assertContains(result, "not approved")
    }

    @Test
    fun testEditFileTool_invalidPath() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = BraveConfirmationHandler()
        val tool = EditFileTool(mockFs, confirmationHandler)

        val result = tool.execute(EditFileTool.Args("../../../etc/passwd", "Hacked"))

        assertContains(result, "Error")
        assertContains(result, "security reasons")
    }

    @Test
    fun testEditFileTool_largeContent() = runTest {
        val mockFs = MockFileSystemProvider()
        val confirmationHandler = BraveConfirmationHandler()
        val tool = EditFileTool(mockFs, confirmationHandler)
        val largeContent = "a".repeat(1_000_001)

        val result = tool.execute(EditFileTool.Args("/test/file.txt", largeContent))

        assertContains(result, "Error")
        assertContains(result, "exceeds maximum size")
    }
}
