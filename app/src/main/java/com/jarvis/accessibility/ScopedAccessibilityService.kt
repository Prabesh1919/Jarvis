package com.jarvis.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jarvis.safety.SafetyLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Representation of a filtered, non-sensitive node on the screen.
 */
data class ScreenNode(
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val className: String?,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean
)

/**
 * Snapshot of the current screen containing the active app package name and non-sensitive nodes.
 */
data class AccessibilitySnapshot(
    val packageName: String,
    val rootNodes: List<ScreenNode>
)

/**
 * OS-compliant, scoped, and secure accessibility service for reading screen state
 * and executing automated actions inside authorized apps.
 */
class ScopedAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
        _isServiceConnected.value = true

        // Safety Monitor: Clear snapshots immediately if automation is disabled
        serviceScope.launch {
            SafetyLayer.isAutomationAllowed.collect { allowed ->
                if (!allowed) {
                    clearSnapshot()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Safety Gate: Do not parse anything if automation is globally disabled
        if (!SafetyLayer.isAutomationAllowed.value) {
            clearSnapshot()
            return
        }

        if (event == null) return

        val root = rootInActiveWindow ?: return
        try {
            val nodesList = mutableListOf<ScreenNode>()
            parseNodeHierarchy(root, nodesList)
            
            _screenSnapshot.value = AccessibilitySnapshot(
                packageName = root.packageName?.toString() ?: event.packageName?.toString() ?: "",
                rootNodes = nodesList
            )
        } finally {
            root.recycle() // Crucial to prevent leaks of root node
        }
    }

    override fun onInterrupt() {
        clearSnapshot()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _isServiceConnected.value = false
        clearSnapshot()
        serviceRef.clear()
    }

    /**
     * Recursively traverses nodes, ignoring any branches or components labeled as sensitive.
     */
    private fun parseNodeHierarchy(node: AccessibilityNodeInfo?, nodesList: MutableList<ScreenNode>) {
        if (node == null) return

        // Bypass sensitive nodes
        if (isSensitiveNode(node)) {
            return
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val screenNode = ScreenNode(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            resourceId = node.viewIdResourceName,
            className = node.className?.toString(),
            bounds = bounds,
            isClickable = node.isClickable,
            isEditable = node.isEditable
        )
        nodesList.add(screenNode)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parseNodeHierarchy(child, nodesList)
                child.recycle() // Recycle child nodes
            }
        }
    }

    /**
     * Checks if a node contains sensitive information indicators.
     */
    private fun isSensitiveNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true

        val textStr = node.text?.toString() ?: ""
        val descStr = node.contentDescription?.toString() ?: ""
        val idStr = node.viewIdResourceName ?: ""
        val hintStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString() ?: ""
        } else {
            ""
        }
        val classNameStr = node.className?.toString() ?: ""

        val sensitiveKeywords = listOf("password", "otp", "cvv", "pin", "financial", "cardnumber", "securitycode")
        return sensitiveKeywords.any { keyword ->
            textStr.contains(keyword, ignoreCase = true) ||
            descStr.contains(keyword, ignoreCase = true) ||
            idStr.contains(keyword, ignoreCase = true) ||
            hintStr.contains(keyword, ignoreCase = true) ||
            classNameStr.contains(keyword, ignoreCase = true)
        }
    }

    private fun clearSnapshot() {
        _screenSnapshot.value = null
    }

    companion object {
        private var serviceRef = WeakReference<ScopedAccessibilityService>(null)

        private val _screenSnapshot = MutableStateFlow<AccessibilitySnapshot?>(null)
        val screenSnapshot: StateFlow<AccessibilitySnapshot?> = _screenSnapshot.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        /**
         * Perform click or set text action programmatically on a node found by its resource ID.
         * Executes only if automation is allowed and node is not sensitive.
         */
        fun performActionOnNode(resourceId: String, action: Int, textInput: String? = null): Boolean {
            if (!SafetyLayer.isAutomationAllowed.value) {
                return false
            }

            val service = serviceRef.get() ?: return false
            val root = service.rootInActiveWindow ?: return false

            try {
                val nodes = root.findAccessibilityNodeInfosByViewId(resourceId) ?: return false
                for (node in nodes) {
                    if (service.isSensitiveNode(node)) {
                        node.recycle()
                        continue
                    }

                    val success = when (action) {
                        AccessibilityNodeInfo.ACTION_CLICK -> {
                            service.performClickCascade(node)
                        }
                        AccessibilityNodeInfo.ACTION_SET_TEXT -> {
                            if (textInput != null) {
                                val arguments = Bundle().apply {
                                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textInput)
                                }
                                node.performAction(action, arguments)
                            } else {
                                false
                            }
                        }
                        else -> {
                            node.performAction(action)
                        }
                    }

                    node.recycle()
                    if (success) {
                        return true
                    }
                }
            } finally {
                root.recycle()
            }
            return false
        }

        /**
         * Cascades click actions up to clickable ancestors if the target node is not clickable directly.
         */
        private fun ScopedAccessibilityService.performClickCascade(node: AccessibilityNodeInfo): Boolean {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        if (current != node) {
                            current.recycle()
                        }
                        return true
                    }
                }
                val parent = current.parent
                if (current != node) {
                    current.recycle()
                }
                current = parent
            }
            return false
        }

        /**
         * Dynamic screenshot capturing, requires Android 11+ (API 30+).
         * Fires callback on the Main executor with the extracted Bitmap.
         */
        fun captureScreen(callback: (Bitmap?) -> Unit) {
            val service = serviceRef.get()
            if (service == null) {
                callback(null)
                return
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    service.takeScreenshot(
                        Display.DEFAULT_DISPLAY,
                        service.mainExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshotResult: ScreenshotResult) {
                                val bitmap = Bitmap.wrapHardwareBuffer(
                                    screenshotResult.hardwareBuffer,
                                    screenshotResult.colorSpace
                                )
                                callback(bitmap)
                            }

                            override fun onFailure(errorCode: Int) {
                                callback(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    callback(null)
                }
            } else {
                callback(null)
            }
        }
    }
}
