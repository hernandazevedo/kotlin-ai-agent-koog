package com.agents.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolValidationTest {

    @Test
    fun testValidatePath_validPath() {
        val result = ToolValidation.validatePath("/valid/path/to/file.txt")
        assertTrue(result.isValid, "Valid path should pass validation")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidatePath_emptyPath() {
        val result = ToolValidation.validatePath("")
        assertFalse(result.isValid, "Empty path should fail validation")
        assertEquals("Path cannot be empty", result.errorMessage)
    }

    @Test
    fun testValidatePath_blankPath() {
        val result = ToolValidation.validatePath("   ")
        assertFalse(result.isValid, "Blank path should fail validation")
        assertEquals("Path cannot be empty", result.errorMessage)
    }

    @Test
    fun testValidatePath_pathTraversalAttack() {
        val result = ToolValidation.validatePath("/some/path/../../../etc/passwd")
        assertFalse(result.isValid, "Path with '..' should fail validation for security")
        assertEquals("Path cannot contain '..' for security reasons", result.errorMessage)
    }

    @Test
    fun testValidatePath_relativePath() {
        val result = ToolValidation.validatePath("relative/path/file.txt")
        assertTrue(result.isValid, "Relative path without '..' should be valid")
    }

    @Test
    fun testValidateContent_validContent() {
        val result = ToolValidation.validateContent("Hello, World!")
        assertTrue(result.isValid, "Valid content should pass validation")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidateContent_emptyContent() {
        val result = ToolValidation.validateContent("")
        assertFalse(result.isValid, "Empty content should fail validation")
        assertEquals("Content cannot be empty", result.errorMessage)
    }

    @Test
    fun testValidateContent_largeContent() {
        val largeContent = "a".repeat(1_000_001) // 1 MB + 1 byte
        val result = ToolValidation.validateContent(largeContent)
        assertFalse(result.isValid, "Content exceeding 1MB should fail validation")
        assertEquals("Content exceeds maximum size of 1MB", result.errorMessage)
    }

    @Test
    fun testValidateContent_atMaxSize() {
        val maxContent = "a".repeat(1_000_000) // Exactly 1 MB
        val result = ToolValidation.validateContent(maxContent)
        assertTrue(result.isValid, "Content at exactly 1MB should pass validation")
    }

    @Test
    fun testValidateContent_multilineContent() {
        val content = """
            Line 1
            Line 2
            Line 3
        """.trimIndent()
        val result = ToolValidation.validateContent(content)
        assertTrue(result.isValid, "Multiline content should pass validation")
    }

    @Test
    fun testValidateContent_specialCharacters() {
        val content = "Special chars: \n\t\r@#$%^&*()"
        val result = ToolValidation.validateContent(content)
        assertTrue(result.isValid, "Content with special characters should pass validation")
    }
}
