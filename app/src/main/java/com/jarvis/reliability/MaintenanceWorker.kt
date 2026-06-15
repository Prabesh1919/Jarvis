package com.jarvis.reliability

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jarvis.memory.MemoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background task manager executing vacuuming, log cleanup, and cache rotations.
 * Enforces strict device state execution limits to protect battery life.
 */
class MaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = MemoryDatabase.getDatabase(applicationContext)

            // 1. Delete conversation messages older than 7 days
            val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            db.openHelper.writableDatabase.execSQL(
                "DELETE FROM conversation_messages WHERE timestamp < $cutoff"
            )

            // 2. Reclaim database file pages
            db.openHelper.writableDatabase.execSQL("VACUUM")

            // 3. Rotate logs
            EncryptedLogger.rotateLogs(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "JarvisMaintenanceWork"

        /**
         * Schedules a periodic maintenance work request.
         * Runs every 24 hours, only when device is charging and connected to unmetered network.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
