package com.jarvis.brain

import android.content.Context
import com.jarvis.personalization.RoutineLearner
import com.jarvis.safety.ActionConfirmationManager
import com.jarvis.safety.SafetyLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ToolCall(
    val toolId: String,
    val arguments: Map<String, Any>
)

sealed class StepResult {
    data class Success(val toolId: String, val result: Any?) : StepResult()
    data class Failure(val toolId: String, val errorMessage: String) : StepResult()
}

data class ExecutionSummary(
    val stepResults: List<StepResult>,
    val completed: Boolean,
    val abortedReason: String? = null
)

/**
 * Sequential execution planner that maps parsed commands to structured tool invocations.
 * Guarantees safety controls by verifying the SafetyLayer state before triggering any tool.
 * Intercepts sensitive actions through the ActionConfirmationManager gate.
 * Records successful executions in RoutineLearner for pattern detection.
 */
object IntentRouter {

    /**
     * Executes a list of tool calls sequentially. Offloaded to Dispatchers.Default.
     * Before starting each tool, it checks the SafetyLayer to ensure automation is allowed.
     * Sensitive tools are routed through the confirmation gate before execution.
     */
    suspend fun executePlan(context: Context, plan: List<ToolCall>): ExecutionSummary = withContext(Dispatchers.Default) {
        val results = mutableListOf<StepResult>()
        var abortedReason: String? = null
        var completed = true

        for ((index, call) in plan.withIndex()) {
            // Check the SafetyLayer before execution
            if (!SafetyLayer.isAutomationAllowed.value) {
                abortedReason = "Automation aborted: Emergency Stop triggered prior to step ${index + 1} (${call.toolId})."
                completed = false
                break
            }

            // Gate sensitive tools through the confirmation manager
            if (ActionConfirmationManager.requiresConfirmation(call.toolId)) {
                ActionConfirmationManager.requestConfirmation(
                    toolId = call.toolId,
                    description = "Jarvis wants to execute: ${call.toolId}",
                    arguments = call.arguments
                )

                // Wait for user response by polling the confirmation result
                val confirmationResult = waitForConfirmation(call.toolId)
                if (confirmationResult == false) {
                    results.add(StepResult.Failure(call.toolId, "Action denied by user confirmation."))
                    continue
                }
            }

            val tool = ToolRegistry.getTool(call.toolId)
            if (tool == null) {
                results.add(StepResult.Failure(call.toolId, "Tool not registered in Registry."))
                continue
            }

            try {
                // Execute tool
                val result = tool.execute(context, call.arguments)
                when (result) {
                    is ToolResult.Success -> {
                        results.add(StepResult.Success(call.toolId, result.output))
                        // Record successful execution for routine learning
                        RoutineLearner.recordExecution(call.toolId)
                    }
                    is ToolResult.Failure -> {
                        results.add(StepResult.Failure(call.toolId, result.errorMessage))
                    }
                }
            } catch (e: Exception) {
                results.add(StepResult.Failure(call.toolId, "Unexpected runtime exception: ${e.localizedMessage}"))
            }
        }

        ExecutionSummary(results, completed, abortedReason)
    }

    /**
     * Polls the confirmation result with a timeout.
     * Returns true if approved, false if denied, null if timed out.
     */
    private suspend fun waitForConfirmation(toolId: String): Boolean? {
        val maxWaitMs = 30_000L // 30 second timeout
        val pollIntervalMs = 200L
        var elapsed = 0L

        while (elapsed < maxWaitMs) {
            val result = ActionConfirmationManager.lastConfirmationResult.value
            if (result != null && result.toolId == toolId) {
                return result.approved
            }
            kotlinx.coroutines.delay(pollIntervalMs)
            elapsed += pollIntervalMs
        }

        return false // Timed out = denied
    }
}

