package com.jarvis.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.jarvis.accessibility.ScopedAccessibilityService
import com.jarvis.safety.SafetyLayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Privacy-first, local screen character recognition engine.
 * Utilizes Google ML Kit Vision to search text layout positions.
 */
object OcrEngine {

    /**
     * Captures active window content via the accessibility service, runs character
     * recognition, and locates the Rect bounding box of target text.
     * Gated by the SafetyLayer.
     */
    suspend fun findTextOnScreen(searchText: String): Rect? {
        if (!SafetyLayer.isAutomationAllowed.value) {
            return null
        }

        // 1. Capture screen bitmap via ScopedAccessibilityService
        val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
            ScopedAccessibilityService.captureScreen { bmp ->
                if (continuation.isActive) {
                    continuation.resume(bmp)
                }
            }
        } ?: return null

        // 2. Process image using offline ML Kit text recognizer
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        var matchedRect: Rect? = null
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                if (line.text.contains(searchText, ignoreCase = true)) {
                                    // Privacy Bypass: Do not extract or automate sensitive words
                                    if (!isSensitiveText(line.text)) {
                                        matchedRect = line.boundingBox
                                        break
                                    }
                                }
                            }
                            if (matchedRect != null) break
                        }
                        if (continuation.isActive) {
                            continuation.resume(matchedRect)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun isSensitiveText(text: String): Boolean {
        val sensitiveKeywords = listOf("password", "otp", "cvv", "pin", "financial", "cardnumber")
        return sensitiveKeywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
    }
}
