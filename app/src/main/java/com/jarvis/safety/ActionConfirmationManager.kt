package com.jarvis.safety

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages user confirmation for sensitive assistant actions.
 * Supports both simple dialog confirmations and biometric (fingerprint/face) authentication gates.
 */
object ActionConfirmationManager {

    /**
     * Tool IDs considered sensitive and requiring explicit user confirmation.
     */
    private val sensitiveToolIds = setOf(
        "make_call",
        "send_sms",
        "click_node",
        "set_node_text"
    )

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    private val _lastConfirmationResult = MutableStateFlow<ConfirmationResult?>(null)
    val lastConfirmationResult: StateFlow<ConfirmationResult?> = _lastConfirmationResult.asStateFlow()

    /**
     * Returns true if a tool ID requires user approval before execution.
     */
    fun requiresConfirmation(toolId: String): Boolean {
        return sensitiveToolIds.contains(toolId)
    }

    /**
     * Posts a confirmation request to the UI layer. The Compose dialog observes
     * [pendingConfirmation] and renders accordingly.
     */
    fun requestConfirmation(toolId: String, description: String, arguments: Map<String, Any>) {
        _pendingConfirmation.value = ConfirmationRequest(
            toolId = toolId,
            description = description,
            arguments = arguments
        )
    }

    /**
     * Called by the UI when the user approves or denies the pending confirmation.
     */
    fun resolveConfirmation(approved: Boolean) {
        _lastConfirmationResult.value = ConfirmationResult(
            toolId = _pendingConfirmation.value?.toolId ?: "",
            approved = approved
        )
        _pendingConfirmation.value = null
    }

    /**
     * Checks if biometric authentication is available on the device.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Triggers a biometric prompt on the provided FragmentActivity.
     * Returns true if authentication succeeded, false otherwise.
     */
    suspend fun authenticateWithBiometric(activity: FragmentActivity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // Don't resume yet — the system allows retries automatically.
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Jarvis: Confirm Action")
                .setSubtitle("Authenticate to proceed with this sensitive action")
                .setNegativeButtonText("Cancel")
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
}

/**
 * Data class representing a pending confirmation request shown to the user.
 */
data class ConfirmationRequest(
    val toolId: String,
    val description: String,
    val arguments: Map<String, Any>
)

/**
 * Data class representing the result of a user confirmation.
 */
data class ConfirmationResult(
    val toolId: String,
    val approved: Boolean
)
