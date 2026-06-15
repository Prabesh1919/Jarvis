package com.jarvis.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class NotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val body: String,
    val postTime: Long
)

/**
 * Service that monitors incoming notifications and manages references to enable quick responses.
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

        // Cache active notifications securely to trigger replies
        private val sbnCache = ConcurrentHashMap<String, StatusBarNotification>()

        fun getCachedSbn(key: String): StatusBarNotification? {
            return sbnCache[key]
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        fetchActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val item = parseNotification(sbn)
        if (item != null) {
            sbnCache[sbn.key] = sbn
            _notifications.value = _notifications.value.filter { it.key != sbn.key } + item
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        sbnCache.remove(sbn.key)
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
    }

    private fun fetchActiveNotifications() {
        runCatching {
            val active = activeNotifications ?: return
            val items = mutableListOf<NotificationItem>()
            sbnCache.clear()
            for (sbn in active) {
                val item = parseNotification(sbn)
                if (item != null) {
                    sbnCache[sbn.key] = sbn
                    items.add(item)
                }
            }
            _notifications.value = items
        }
    }

    private fun parseNotification(sbn: StatusBarNotification): NotificationItem? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        // Skip empty or system alerts
        if (title.isBlank() && text.isBlank()) return null
        
        return NotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            title = title,
            body = text,
            postTime = sbn.postTime
        )
    }
}
