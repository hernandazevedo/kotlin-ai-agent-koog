package com.agents.validation

/**
 * Validation utilities for tool parameters
 * Based on JetBrains Koog best practices
 */
object ToolValidation {

    /**
     * Validates a file path parameter
     * @return ValidationResult with error message if invalid
     */
    fun validatePath(path: String): ValidationResult {
        return when {
            path.isBlank() -> ValidationResult.Invalid("Path cannot be empty")
            path.contains("..") -> ValidationResult.Invalid("Path cannot contain '..' for security reasons")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates file content for potential issues
     * @return ValidationResult with error message if invalid
     */
    fun validateContent(content: String): ValidationResult {
        return when {
            content.isEmpty() -> ValidationResult.Invalid("Content cannot be empty")
            content.length > 1_000_000 -> ValidationResult.Invalid("Content exceeds maximum size of 1MB")
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Result of a validation check
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val errorMessage: String? get() = (this as? Invalid)?.message
}
