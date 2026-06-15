package com.jarvis.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val label: String, val packageName: String)

/**
 * Tool for searching and launching installed applications, as well as navigating to system settings.
 * Designed to offload heavy package listing calls to background threads (Dispatchers.IO).
 */
object AppLauncherTool {

    /**
     * Retrieves all installed applications with a launch intent.
     * Offloaded to Dispatchers.IO to maintain a smooth 60/120 FPS UI.
     */
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppInfo>()
        
        for (app in apps) {
            // Only include launchable applications
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                val label = app.loadLabel(pm).toString()
                result.add(AppInfo(label, app.packageName))
            }
        }
        // Sort alphabetically by app name
        result.sortBy { it.label.lowercase() }
        result
    }

    /**
     * Launches an app using its package name.
     * Returns true if successfully launched, false if the app has no launch intent or fails to launch.
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Deep-links to a specific System Settings panel.
     */
    fun openSettingsPanel(context: Context, settingsAction: String): Boolean {
        val intent = Intent(settingsAction)
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    // Common system settings targets
    const val SETTINGS_MAIN = Settings.ACTION_SETTINGS
    const val SETTINGS_WIFI = Settings.ACTION_WIFI_SETTINGS
    const val SETTINGS_BLUETOOTH = Settings.ACTION_BLUETOOTH_SETTINGS
    const val SETTINGS_ACCESSIBILITY = Settings.ACTION_ACCESSIBILITY_SETTINGS
    const val SETTINGS_BATTERY = Settings.ACTION_BATTERY_SAVER_SETTINGS
    const val SETTINGS_LOCATION = Settings.ACTION_LOCATION_SOURCE_SETTINGS
}
