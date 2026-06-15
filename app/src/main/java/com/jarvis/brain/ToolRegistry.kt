package com.jarvis.brain

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.jarvis.accessibility.ScopedAccessibilityService
import com.jarvis.tools.AppLauncherTool
import com.jarvis.tools.PhoneActionsTool
import com.jarvis.vision.OcrEngine
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry managing available tools for the assistant.
 * Registers platform intents tools automatically on initialization.
 */
object ToolRegistry {
    private val tools = ConcurrentHashMap<String, Tool>()

    init {
        // Register default platform tools
        registerTool(LaunchAppTool)
        registerTool(MakeCallTool)
        registerTool(SendSMSTool)
        registerTool(SetAlarmTool)
        registerTool(ToggleFlashlightTool)
        registerTool(ClickNodeTool)
        registerTool(SetNodeTextTool)
        registerTool(FindTextOnScreenTool)
        registerTool(QueryDocumentsTool)
    }

    fun registerTool(tool: Tool) {
        tools[tool.id] = tool
    }

    fun unregisterTool(toolId: String) {
        tools.remove(toolId)
    }

    fun getTool(toolId: String): Tool? {
        return tools[toolId]
    }

    fun getAllTools(): Collection<Tool> {
        return tools.values
    }

    // Helper functions for robust type extraction and type safety
    private fun extractString(args: Map<String, Any>, key: String): String? {
        return args[key]?.toString()
    }

    private fun extractInt(args: Map<String, Any>, key: String): Int? {
        val value = args[key] ?: return null
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun extractBoolean(args: Map<String, Any>, key: String): Boolean? {
        val value = args[key] ?: return null
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }

    // --- DEFAULT TOOL IMPLEMENTATIONS ---

    private object LaunchAppTool : Tool {
        override val id: String = "launch_app"
        override val name: String = "Launch App"
        override val description: String = "Opens an installed application using its package name."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "packageName" to ToolParameter("string", "The package name of the app to launch (e.g. com.android.settings)")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val packageName = extractString(args, "packageName")
                ?: return ToolResult.Failure("Missing required parameter 'packageName'")
            
            val success = AppLauncherTool.launchApp(context, packageName)
            return if (success) {
                ToolResult.Success("App $packageName launched successfully.")
            } else {
                ToolResult.Failure("Failed to launch app $packageName. Package may not exist or has no launcher intent.")
            }
        }
    }

    private object MakeCallTool : Tool {
        override val id: String = "make_call"
        override val name: String = "Make Call"
        override val description: String = "Starts a phone call or opens dialer to start a call."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "phoneNumber" to ToolParameter("string", "The phone number to dial")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val phoneNumber = extractString(args, "phoneNumber")
                ?: return ToolResult.Failure("Missing required parameter 'phoneNumber'")

            val success = PhoneActionsTool.makeCall(context, phoneNumber)
            return if (success) {
                ToolResult.Success("Call initiated to $phoneNumber.")
            } else {
                ToolResult.Failure("Failed to launch dialer/call intent for $phoneNumber.")
            }
        }
    }

    private object SendSMSTool : Tool {
        override val id: String = "send_sms"
        override val name: String = "Send SMS"
        override val description: String = "Pre-populates and opens the SMS sender app."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "phoneNumber" to ToolParameter("string", "The recipient's phone number"),
            "message" to ToolParameter("string", "The SMS body message")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val phoneNumber = extractString(args, "phoneNumber")
                ?: return ToolResult.Failure("Missing required parameter 'phoneNumber'")
            val message = extractString(args, "message")
                ?: return ToolResult.Failure("Missing required parameter 'message'")

            val success = PhoneActionsTool.sendSMS(context, phoneNumber, message)
            return if (success) {
                ToolResult.Success("SMS composer opened for $phoneNumber.")
            } else {
                ToolResult.Failure("Failed to resolve SMS composer intent.")
            }
        }
    }

    private object SetAlarmTool : Tool {
        override val id: String = "set_alarm"
        override val name: String = "Set Alarm"
        override val description: String = "Sets a system clock alarm."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "hour" to ToolParameter("int", "Hour of the alarm (0-23)"),
            "minute" to ToolParameter("int", "Minute of the alarm (0-59)"),
            "message" to ToolParameter("string", "Optional label for the alarm", required = false)
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val hour = extractInt(args, "hour")
                ?: return ToolResult.Failure("Missing or invalid required parameter 'hour' (int)")
            val minute = extractInt(args, "minute")
                ?: return ToolResult.Failure("Missing or invalid required parameter 'minute' (int)")
            val message = extractString(args, "message") ?: "Assistant Alarm"

            if (hour !in 0..23 || minute !in 0..59) {
                return ToolResult.Failure("Invalid bounds: hour must be 0-23, minute 0-59.")
            }

            val success = PhoneActionsTool.setAlarm(context, hour, minute, message)
            return if (success) {
                ToolResult.Success("Alarm set for $hour:$minute with label '$message'.")
            } else {
                ToolResult.Failure("Failed to resolve alarm settings intent.")
            }
        }
    }

    private object ToggleFlashlightTool : Tool {
        override val id: String = "toggle_flashlight"
        override val name: String = "Toggle Flashlight"
        override val description: String = "Turns the device camera flashlight (torch) on or off."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "enabled" to ToolParameter("boolean", "True to enable torch, false to disable")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val enabled = extractBoolean(args, "enabled")
                ?: return ToolResult.Failure("Missing or invalid required parameter 'enabled' (boolean)")

            val success = PhoneActionsTool.toggleFlashlight(context, enabled)
            return if (success) {
                ToolResult.Success("Flashlight state set to $enabled.")
            } else {
                ToolResult.Failure("Failed to toggle flashlight hardware.")
            }
        }
    }

    private object ClickNodeTool : Tool {
        override val id: String = "click_node"
        override val name: String = "Click Screen Node"
        override val description: String = "Clicks a button or view on the screen using its resource ID, if permitted by Safety Layer."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "resourceId" to ToolParameter("string", "The view resource ID of the target element (e.g. com.whatsapp:id/send_button)")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val resourceId = extractString(args, "resourceId")
                ?: return ToolResult.Failure("Missing required parameter 'resourceId'")

            val success = ScopedAccessibilityService.performActionOnNode(
                resourceId = resourceId,
                action = AccessibilityNodeInfo.ACTION_CLICK
            )
            return if (success) {
                ToolResult.Success("Successfully clicked node: $resourceId")
            } else {
                ToolResult.Failure("Failed to click node: $resourceId. Node may not be visible, clickable, or automation is disabled.")
            }
        }
    }

    private object SetNodeTextTool : Tool {
        override val id: String = "set_node_text"
        override val name: String = "Set Text on Screen Node"
        override val description: String = "Inputs text into an editable view on the screen using its resource ID."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "resourceId" to ToolParameter("string", "The view resource ID of the editable target element"),
            "text" to ToolParameter("string", "The text content to input")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val resourceId = extractString(args, "resourceId")
                ?: return ToolResult.Failure("Missing required parameter 'resourceId'")
            val text = extractString(args, "text")
                ?: return ToolResult.Failure("Missing required parameter 'text'")

            val success = ScopedAccessibilityService.performActionOnNode(
                resourceId = resourceId,
                action = AccessibilityNodeInfo.ACTION_SET_TEXT,
                textInput = text
            )
            return if (success) {
                ToolResult.Success("Successfully set text on node: $resourceId")
            } else {
                ToolResult.Failure("Failed to set text on node: $resourceId. Node may not be visible, editable, or automation is disabled.")
            }
        }
    }

    private object FindTextOnScreenTool : Tool {
        override val id: String = "find_text_on_screen"
        override val name: String = "Find Text on Screen (OCR)"
        override val description: String = "Scans the screen using offline OCR to find the bounding box coordinates of target text."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "searchText" to ToolParameter("string", "The text label to search for on the screen")
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val searchText = extractString(args, "searchText")
                ?: return ToolResult.Failure("Missing required parameter 'searchText'")

            val bounds = OcrEngine.findTextOnScreen(searchText)
            return if (bounds != null) {
                ToolResult.Success(mapOf(
                    "found" to true,
                    "left" to bounds.left,
                    "top" to bounds.top,
                    "right" to bounds.right,
                    "bottom" to bounds.bottom,
                    "centerX" to bounds.centerX(),
                    "centerY" to bounds.centerY()
                ))
            } else {
                ToolResult.Failure("Text '$searchText' not found on the active screen, or automation is disabled.")
            }
        }
    }

    private object QueryDocumentsTool : Tool {
        override val id: String = "query_documents"
        override val name: String = "Query Local Documents"
        override val description: String = "Searches offline user documents using local RAG indexes to answer queries."
        override val parameters: Map<String, ToolParameter> = mapOf(
            "query" to ToolParameter("string", "The semantic query or search keywords"),
            "limit" to ToolParameter("int", "Maximum number of relevance snippets to return", required = false)
        )

        override suspend fun execute(context: Context, args: Map<String, Any>): ToolResult {
            val query = extractString(args, "query")
                ?: return ToolResult.Failure("Missing required parameter 'query'")
            val limit = extractInt(args, "limit") ?: 3

            val passages = com.jarvis.knowledge.LocalRagEngine.queryRag(context, query, limit)
            return if (passages.isNotEmpty()) {
                ToolResult.Success(mapOf(
                    "passages" to passages
                ))
            } else {
                ToolResult.Success(mapOf(
                    "passages" to emptyList<String>(),
                    "message" to "No relevant document passages found."
                ))
            }
        }
    }
}
