package com.jarvis.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.core.content.ContextCompat

/**
 * Handles basic phone actions using standard system Intents and native managers.
 * Ensures graceful fallback on permission denials, and executes without blocking the UI.
 */
object PhoneActionsTool {

    /**
     * Initiates a phone call. If Manifest.permission.CALL_PHONE is granted, starts a direct call.
     * Otherwise, falls back gracefully to opening the phone dialer.
     */
    fun makeCall(context: Context, phoneNumber: String): Boolean {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        }
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Opens the default SMS app with the recipient number and message pre-populated.
     */
    fun sendSMS(context: Context, phoneNumber: String, message: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Sets an alarm at the specified hour and minute.
     * Parameters: hour (0-23), minutes (0-59), message (label).
     */
    fun setAlarm(context: Context, hour: Int, minutes: Int, message: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Inserts a calendar event (reminder).
     */
    fun createReminder(
        context: Context,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
        }
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Opens the system camera.
     */
    fun openCamera(context: Context): Boolean {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return IntentResolverHelper.launchIntentSafely(context, intent)
    }

    /**
     * Toggles the device's camera flashlight (torch) state.
     * Uses CameraManager to modify state without holding active wake/device locks.
     */
    fun toggleFlashlight(context: Context, enabled: Boolean): Boolean {
        return runCatching {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            true
        }.getOrElse {
            false
        }
    }

    /**
     * Dispatches media actions directly to the active system session.
     */
    fun dispatchMediaAction(context: Context, action: MediaAction): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return false
        val keyCode = when (action) {
            MediaAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }
        return runCatching {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            true
        }.getOrElse {
            false
        }
    }

    enum class MediaAction {
        PLAY_PAUSE, NEXT, PREVIOUS, STOP
    }
}
