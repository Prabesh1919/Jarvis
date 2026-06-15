package com.jarvis.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Utility helper to query, resolve, and trigger Android Intents safely.
 * Prevents ActivityNotFoundException crashes and respects modern Android execution limits.
 */
object IntentResolverHelper {

    /**
     * Checks if there is any application/activity on the device capable of handling this intent.
     * Handles Android 11+ package visibility boundaries gracefully.
     */
    fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val componentName = intent.resolveActivity(packageManager)
        if (componentName != null) return true

        // Fallback for older versions or explicit declarations
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved.isNotEmpty()
    }

    /**
     * Safely launches the provided intent. Handles contexts outside of an Activity by appending
     * the FLAG_ACTIVITY_NEW_TASK flag automatically.
     * Returns true if launch succeeded, false if resolve failed.
     */
    fun launchIntentSafely(context: Context, intent: Intent): Boolean {
        return runCatching {
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrElse {
            false
        }
    }
}
