package com.jarvis.brain

import android.content.Context

/**
 * Result states returned by execution of a Tool.
 */
sealed class ToolResult {
    data class Success(val output: Any?) : ToolResult()
    data class Failure(val errorMessage: String, val error: Throwable? = null) : ToolResult()
}

/**
 * Description of a parameter required by a Tool, allowing programmatic validation.
 */
data class ToolParameter(
    val type: String, // "string", "int", "boolean"
    val description: String,
    val required: Boolean = true
)

/**
 * Interface that all assistant capabilities (tools) must implement.
 * Decouples action dispatching from internal details.
 */
interface Tool {
    val id: String
    val name: String
    val description: String
    val parameters: Map<String, ToolParameter>

    /**
     * Executes the tool logic in a non-blocking coroutine.
     */
    suspend fun execute(context: Context, args: Map<String, Any>): ToolResult
}
