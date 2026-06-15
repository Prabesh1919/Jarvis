package com.jarvis.context

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceContext(
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    val networkType: String = "UNKNOWN", // "WIFI", "CELLULAR", "NONE", "UNKNOWN"
    val activeAppPackage: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Reactive context collector tracking device status metrics (Network, Battery, Location, Active App).
 * Fully non-blocking, memory-leak-safe, and utilizes thread-safe state flows.
 */
object ContextEngine {

    private val _deviceContext = MutableStateFlow(DeviceContext())
    val deviceContext: StateFlow<DeviceContext> = _deviceContext.asStateFlow()

    private var batteryReceiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    private val ioScope = CoroutineScope(Dispatchers.IO)

    /**
     * Starts monitoring system broadcasts and callbacks. Call this on application or active scope launch.
     */
    @Synchronized
    fun startMonitoring(context: Context) {
        val appContext = context.applicationContext
        
        // 1. Setup Battery Tracking
        if (batteryReceiver == null) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    
                    val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                   status == BatteryManager.BATTERY_STATUS_FULL
                    
                    _deviceContext.value = _deviceContext.value.copy(
                        batteryPct = pct,
                        isCharging = charging,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
            appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiver = receiver
        }

        // 2. Setup Network Tracking
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (networkCallback == null && connectivityManager != null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    updateNetworkType(appContext)
                }
                override fun onLost(network: android.net.Network) {
                    updateNetworkType(appContext)
                }
                override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: NetworkCapabilities) {
                    updateNetworkType(appContext)
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        }

        // Trigger immediate background sync
        refreshDynamicContext(appContext)
    }

    /**
     * Stops monitoring and releases system hooks to prevent resource leaks.
     */
    @Synchronized
    fun stopMonitoring(context: Context) {
        val appContext = context.applicationContext
        
        batteryReceiver?.let {
            appContext.unregisterReceiver(it)
            batteryReceiver = null
        }

        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    /**
     * Refreshes location and active app info in a non-blocking background thread.
     */
    fun refreshDynamicContext(context: Context) {
        ioScope.launch {
            val activeApp = getActiveAppPackage(context)
            val location = getDeviceLocation(context)
            
            _deviceContext.value = _deviceContext.value.copy(
                activeAppPackage = activeApp,
                latitude = location?.first,
                longitude = location?.second,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun updateNetworkType(context: Context) {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var type = "NONE"

        connManager?.let { cm ->
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            
            capabilities?.let { cap ->
                type = when {
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    else -> "UNKNOWN"
                }
            }
        }

        _deviceContext.value = _deviceContext.value.copy(
            networkType = type,
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun getActiveAppPackage(context: Context): String? = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@withContext null
        val time = System.currentTimeMillis()
        
        // Query tasks from the last 15 seconds
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 15,
            time
        )
        if (stats.isNullOrEmpty()) return@withContext null
        
        // Fetch the app package with the latest active interaction timestamp
        stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private suspend fun getDeviceLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
        val providers = locationManager.getProviders(true)
        var bestLocation: android.location.Location? = null

        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        bestLocation?.let { Pair(it.latitude, it.longitude) }
    }
}
