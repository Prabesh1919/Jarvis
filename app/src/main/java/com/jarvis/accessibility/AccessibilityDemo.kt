package com.jarvis.accessibility

import android.content.Context
import com.jarvis.brain.IntentRouter
import com.jarvis.brain.ToolCall
import com.jarvis.safety.SafetyLayer

/**
 * Example demonstrating how Scoped Accessibility actions are executed via the IntentRouter.
 */
object AccessibilityDemo {

    /**
     * Simulates executing a plan to type text into WhatsApp and click the send button.
     * Gated by the SafetyLayer, and statically restricted to package lists in the XML config.
     */
    suspend fun runDemoWorkflow(context: Context) {
        // Enforce safety checks
        if (!SafetyLayer.isAutomationAllowed.value) {
            println("Demo aborted: Automation is currently disabled by SafetyLayer.")
            return
        }

        // 1. Define sequential tool calls
        val plan = listOf(
            ToolCall(
                toolId = "set_node_text",
                arguments = mapOf(
                    "resourceId" to "com.whatsapp:id/entry",
                    "text" to "Hello, this is an automated message from Jarvis!"
                )
            ),
            ToolCall(
                toolId = "click_node",
                arguments = mapOf(
                    "resourceId" to "com.whatsapp:id/send"
                )
            )
        )

        // 2. Execute plan via the IntentRouter
        val summary = IntentRouter.executePlan(context, plan)
        
        println("Workflow execution complete: Completed = ${summary.completed}")
        summary.stepResults.forEach { result ->
            println("- $result")
        }
    }
}
