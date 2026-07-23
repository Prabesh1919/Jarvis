package com.jarvis.agent

import org.json.JSONArray
import org.json.JSONObject

enum class TaskState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

data class TaskStep(
    val id: Int,
    val description: String,
    val toolName: String,
    val parameters: JSONObject,
    var state: TaskState = TaskState.PENDING,
    var result: String? = null
)

data class ExecutionPlan(
    val userGoal: String,
    val steps: List<TaskStep>
)

object TaskPlanner {

    /**
     * Parses a multi-step JSON plan from an LLM response.
     */
    fun parsePlan(userGoal: String, jsonResponse: String): ExecutionPlan {
        val stepsList = mutableListOf<TaskStep>()
        try {
            val json = JSONObject(jsonResponse)
            val stepsArr = json.optJSONArray("steps") ?: JSONArray()
            
            for (i in 0 until stepsArr.length()) {
                val stepObj = stepsArr.getJSONObject(i)
                val id = stepObj.optInt("id", i + 1)
                val desc = stepObj.optString("description", "Execute step ${i + 1}")
                val tool = stepObj.optString("tool", "generic_action")
                val params = stepObj.optJSONObject("parameters") ?: JSONObject()

                stepsList.add(TaskStep(id, desc, tool, params))
            }
        } catch (e: Exception) {
            // Fallback: single execution step
            stepsList.add(TaskStep(1, userGoal, "intent_router", JSONObject().put("goal", userGoal)))
        }

        return ExecutionPlan(userGoal, stepsList)
    }
}
