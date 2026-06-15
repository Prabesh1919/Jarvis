package com.jarvis.workflow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.jarvis.brain.ToolCall
import com.jarvis.brain.ToolRegistry
import com.jarvis.brain.ToolResult
import com.jarvis.memory.MemoryManager
import com.jarvis.memory.WorkflowState
import com.jarvis.safety.SafetyLayer
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Orchestrates multi-step automation tasks, tracking progress inside Room to survive crashes,
 * retrying failing steps via exponential backoff, and alerting the user if actions stall.
 */
object WorkflowCoordinator {

    /**
     * Schedules a workflow inside a coroutine job and registers it to the safety manager.
     */
    fun startWorkflow(
        context: Context,
        workflowId: String,
        workflowName: String,
        steps: List<ToolCall>,
        scope: CoroutineScope
    ): Job {
        val job = scope.launch(Dispatchers.Default) {
            executeWorkflow(context, workflowId, workflowName, steps)
        }
        SafetyLayer.registerJob(workflowId, job)
        return job
    }

    /**
     * Main execution runner. Persists state details before running each step.
     */
    suspend fun executeWorkflow(
        context: Context,
        workflowId: String,
        workflowName: String,
        steps: List<ToolCall>
    ): Boolean {
        if (!SafetyLayer.isAutomationAllowed.value) {
            saveState(workflowId, workflowName, 0, "FAILED", steps)
            return false
        }

        saveState(workflowId, workflowName, 0, "RUNNING", steps)

        for (i in steps.indices) {
            val step = steps[i]

            // Record execution checkpoint
            saveState(workflowId, workflowName, i, "RUNNING", steps)

            var success = false
            var attempts = 0
            val maxRetries = 3

            while (attempts <= maxRetries && !success) {
                // Pre-step safety check
                if (!SafetyLayer.isAutomationAllowed.value) {
                    saveState(workflowId, workflowName, i, "FAILED", steps)
                    return false
                }

                try {
                    val tool = ToolRegistry.getTool(step.toolId)
                    if (tool == null) {
                        break
                    }

                    val result = tool.execute(context, step.arguments)
                    if (result is ToolResult.Success) {
                        success = true
                    } else {
                        attempts++
                        if (attempts <= maxRetries) {
                            val backoffMs = 500L * (1 shl (attempts - 1)) // 500ms, 1000ms, 2000ms
                            delay(backoffMs)
                        }
                    }
                } catch (e: Exception) {
                    attempts++
                    if (attempts <= maxRetries) {
                        val backoffMs = 500L * (1 shl (attempts - 1))
                        delay(backoffMs)
                    }
                }
            }

            if (!success) {
                // Retries exhausted: trigger human-in-the-loop fallback
                saveState(workflowId, workflowName, i, "PAUSED_WAITING_USER", steps)
                postUserAlertNotification(context, workflowId, workflowName, i)
                return false
            }
        }

        saveState(workflowId, workflowName, steps.size, "COMPLETED", steps)
        return true
    }

    private suspend fun saveState(
        id: String,
        name: String,
        index: Int,
        status: String,
        steps: List<ToolCall>
    ) {
        val serialized = serializeSteps(steps)
        val state = WorkflowState(id, name, index, status, serialized)
        MemoryManager.saveWorkflowState(state)
    }

    private fun postUserAlertNotification(
        context: Context,
        workflowId: String,
        workflowName: String,
        failedStepIndex: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "workflow_alerts"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Workflow Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for paused or failed automations."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }

        builder.setContentTitle("Automation Paused: Action Required")
            .setContentText("Workflow '$workflowName' paused at step ${failedStepIndex + 1}. Tap to verify.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)

        notificationManager.notify(workflowId.hashCode(), builder.build())
    }

    fun serializeSteps(steps: List<ToolCall>): String {
        val array = JSONArray()
        for (step in steps) {
            val obj = JSONObject()
            obj.put("toolId", step.toolId)
            val argsObj = JSONObject()
            for ((key, value) in step.arguments) {
                argsObj.put(key, value)
            }
            obj.put("arguments", argsObj)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeSteps(serialized: String): List<ToolCall> {
        val list = mutableListOf<ToolCall>()
        try {
            val array = JSONArray(serialized)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val toolId = obj.getString("toolId")
                val argsObj = obj.getJSONObject("arguments")
                val arguments = mutableMapOf<String, Any>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    arguments[key] = argsObj.get(key)
                }
                list.add(ToolCall(toolId, arguments))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
