package com.aether.launcher.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.engine.notification.AetherNotification

class AetherNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        AetherRuntime.initialize(applicationContext)
        activeNotifications?.forEach(::ingest)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = ingest(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!AetherRuntime.isInitialized()) return
        AetherRuntime.registry.notifications.remove(sbn.id, sbn.packageName)
        val current = AetherRuntime.registry.island.activity
        if (current.key == keyFor(sbn)) {
            AetherRuntime.registry.notifications.latest()?.let { latest ->
                AetherRuntime.registry.island.setActivity(
                    IslandActivity(ActivityType.NOTIFICATION, latest.title.ifBlank { "Activity" }, latest.text, 0, keyFor(latest.id, latest.packageName))
                )
            } ?: AetherRuntime.registry.island.clearActivity()
        }
    }

    private fun ingest(sbn: StatusBarNotification) {
        if (!AetherRuntime.isInitialized()) AetherRuntime.initialize(applicationContext)
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val type = when {
            n.category == Notification.CATEGORY_CALL -> ActivityType.CALL
            n.category == Notification.CATEGORY_ALARM || n.category == Notification.CATEGORY_EVENT -> ActivityType.TIMER
            n.category == Notification.CATEGORY_TRANSPORT || n.category == Notification.CATEGORY_PROGRESS -> ActivityType.MEDIA
            n.category == Notification.CATEGORY_SERVICE -> ActivityType.RECORDING
            else -> ActivityType.NOTIFICATION
        }
        val key = keyFor(sbn)
        AetherRuntime.registry.notifications.add(AetherNotification(sbn.id, sbn.packageName, title, text, sbn.postTime))
        AetherRuntime.registry.island.setActivity(
            IslandActivity(type, title.ifBlank { "Activity" }, text, if (type == ActivityType.CALL) 20 else 0, key)
        )
    }

    private fun keyFor(sbn: StatusBarNotification) = keyFor(sbn.id, sbn.packageName)
    private fun keyFor(id: Int, packageName: String) = "$packageName:$id"
}
