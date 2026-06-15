package com.jarvis.safety

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

enum class PermissionType {
    MICROPHONE,
    NOTIFICATIONS,
    USAGE_STATS
}

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
    object NotRequested : PermissionState()
}

/**
 * Centralized Permission Manager that orchestrates system permissions using modern Activity Result APIs.
 * Prevents memory leaks by using WeakReferences and Lifecycle observers.
 * Fully thread-safe, non-blocking, and idempotent.
 */
object PermissionManager {

    private val _microphoneState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val microphoneState: StateFlow<PermissionState> = _microphoneState.asStateFlow()

    private val _notificationsState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val notificationsState: StateFlow<PermissionState> = _notificationsState.asStateFlow()

    private val _usageStatsState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val usageStatsState: StateFlow<PermissionState> = _usageStatsState.asStateFlow()

    private var activityRef: WeakReference<ComponentActivity>? = null
    
    private var micLauncher: ActivityResultLauncher<String>? = null
    private var notificationsLauncher: ActivityResultLauncher<String>? = null

    /**
     * Binds the ComponentActivity to the PermissionManager. Call this inside the Activity's onCreate().
     * This registers the required ActivityResultLaunchers dynamically.
     */
    @Synchronized
    fun register(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
        
        micLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            updatePermissionState(activity, PermissionType.MICROPHONE, isGranted)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                updatePermissionState(activity, PermissionType.NOTIFICATIONS, isGranted)
            }
        }

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // Instantly break references to prevent leaks on destruction/recreation
                synchronized(this@PermissionManager) {
                    activityRef = null
                    micLauncher = null
                    notificationsLauncher = null
                }
                super.onDestroy(owner)
            }
        })

        // Instantly synchronise current status
        checkPermissionState(activity, PermissionType.MICROPHONE)
        checkPermissionState(activity, PermissionType.NOTIFICATIONS)
        checkPermissionState(activity, PermissionType.USAGE_STATS)
    }

    /**
     * Inspects permission states reactively and publishes them.
     */
    fun checkPermissionState(context: Context, type: PermissionType): PermissionState {
        val state = when (type) {
            PermissionType.MICROPHONE -> {
                checkRuntimePermission(context, Manifest.permission.RECORD_AUDIO)
            }
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    checkRuntimePermission(context, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionState.Granted // notifications granted by default prior to Android 13
                }
            }
            PermissionType.USAGE_STATS -> {
                if (hasUsageStatsPermission(context)) PermissionState.Granted else PermissionState.Denied
            }
        }

        when (type) {
            PermissionType.MICROPHONE -> _microphoneState.value = state
            PermissionType.NOTIFICATIONS -> _notificationsState.value = state
            PermissionType.USAGE_STATS -> _usageStatsState.value = state
        }
        return state
    }

    private fun checkRuntimePermission(context: Context, permission: String): PermissionState {
        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) return PermissionState.Granted

        val activity = activityRef?.get()
        return if (activity != null) {
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
            if (shouldShowRationale) PermissionState.Denied else PermissionState.NotRequested
        } else {
            PermissionState.Denied
        }
    }

    private fun updatePermissionState(context: Context, type: PermissionType, isGranted: Boolean) {
        val state = if (isGranted) {
            PermissionState.Granted
        } else {
            val permissionStr = when (type) {
                PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
                PermissionType.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
                else -> null
            }
            
            val activity = activityRef?.get()
            if (permissionStr != null && activity != null && !activity.shouldShowRequestPermissionRationale(permissionStr)) {
                PermissionState.PermanentlyDenied
            } else {
                PermissionState.Denied
            }
        }

        when (type) {
            PermissionType.MICROPHONE -> _microphoneState.value = state
            PermissionType.NOTIFICATIONS -> _notificationsState.value = state
            PermissionType.USAGE_STATS -> _usageStatsState.value = state
        }
    }

    /**
     * Dispatches the system prompt request for a PermissionType. Safe and idempotent.
     */
    fun requestPermission(type: PermissionType) {
        val activity = activityRef?.get() ?: return
        
        when (type) {
            PermissionType.MICROPHONE -> {
                micLauncher?.launch(Manifest.permission.RECORD_AUDIO)
                    ?: checkPermissionState(activity, PermissionType.MICROPHONE)
            }
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                        ?: checkPermissionState(activity, PermissionType.NOTIFICATIONS)
                } else {
                    _notificationsState.value = PermissionState.Granted
                }
            }
            PermissionType.USAGE_STATS -> {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching {
                    activity.startActivity(intent)
                }.onFailure {
                    // Fallback if packages can't filter Settings directly
                    val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    activity.startActivity(fallbackIntent)
                }
            }
        }
    }

    /**
     * Direct deep-link settings access for graceful degradation if permanently denied.
     */
    fun openAppSettings() {
        val activity = activityRef?.get() ?: return
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
