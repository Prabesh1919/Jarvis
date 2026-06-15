package com.jarvis.notifications

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification

/**
 * Helper to execute direct quick replies to incoming notifications programmatic-style.
 * Integrates with standard RemoteInput bindings to interact with apps (e.g. WhatsApp, Messages).
 */
object NotificationReplyHelper {

    /**
     * Sends a direct quick reply message to the notification associated with the cached StatusBarNotification.
     * Returns true if reply succeeded, false if no direct reply action could be resolved.
     */
    fun sendQuickReply(context: Context, sbn: StatusBarNotification, replyText: String): Boolean {
        val notification = sbn.notification
        val actions = notification.actions ?: return false

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.resultKey != null) {
                    return try {
                        val resultsBundle = Bundle().apply {
                            putCharSequence(remoteInput.resultKey, replyText)
                        }
                        val intent = Intent().apply {
                            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        }
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, resultsBundle)
                        
                        // Fire the pending intent with the reply payload
                        action.actionIntent.send(context, 0, intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
        return false
    }
}
