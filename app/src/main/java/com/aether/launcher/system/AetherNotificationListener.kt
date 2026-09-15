package com.aether.launcher.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.engine.notification.AetherNotification

/** Optional notification bridge. The user must explicitly grant Notification Access. */
class AetherNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        AetherRuntime.initialize(applicationContext)
        activeNotifications?.forEach(::ingest)
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) = ingest(sbn)
    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit

    private fun ingest(sbn: StatusBarNotification) {
        if (!AetherRuntime.isInitialized()) AetherRuntime.initialize(applicationContext)
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        AetherRuntime.registry.notifications.add(
            AetherNotification(sbn.id, sbn.packageName, title, text, sbn.postTime)
        )
        val type = when {
            n.category == Notification.CATEGORY_CALL -> ActivityType.CALL
            n.category == Notification.CATEGORY_ALARM || n.category == Notification.CATEGORY_EVENT -> ActivityType.TIMER
            n.category == Notification.CATEGORY_TRANSPORT -> ActivityType.MEDIA
            else -> ActivityType.NOTIFICATION
        }
        AetherRuntime.registry.island.setActivity(
            IslandActivity(type, title.ifBlank { "Activity" }, text, 0)
        )
    }
}
