package com.aether.launcher.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.engine.notification.AetherNotification

/**
 * Real notification bridge.
 * Aggressively maps call / media / progress / recording notifications into the Island
 * so the capsule appears over any app, not only Home.
 */
class AetherNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        AetherRuntime.initialize(applicationContext)
        activeNotifications?.forEach(::ingest)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = ingest(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!AetherRuntime.isInitialized()) return
        NotificationReplyBridge.forget(sbn)
        AetherRuntime.registry.notifications.remove(sbn.id, sbn.packageName)

        val cur = AetherRuntime.registry.island.activity
        if (cur.key == keyFor(sbn)) {
            // Fall back to next highest-priority live notification
            val next = AetherRuntime.registry.notifications.latest()
            if (next != null) {
                AetherRuntime.registry.island.setActivity(
                    IslandActivity(
                        type = ActivityType.NOTIFICATION,
                        title = next.title.ifBlank { "Activity" },
                        detail = next.text,
                        priority = 10,
                        key = keyFor(next.id, next.packageName),
                        packageName = next.packageName
                    )
                )
            } else {
                AetherRuntime.registry.island.clearActivity()
            }
        }
        AetherOverlayService.refreshFromSystem()
    }

    private fun ingest(sbn: StatusBarNotification) {
        if (!AetherRuntime.isInitialized()) AetherRuntime.initialize(applicationContext)

        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val sub = n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val progress = n.extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = n.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)

        val type = when {
            n.category == Notification.CATEGORY_CALL -> ActivityType.CALL
            n.category == Notification.CATEGORY_TRANSPORT ||
                n.category == Notification.CATEGORY_PROGRESS ||
                isMediaPackage(sbn.packageName) -> {
                if (progressMax > 0 && progress >= 0) ActivityType.DOWNLOAD else ActivityType.MEDIA
            }
            n.category == Notification.CATEGORY_ALARM ||
                n.category == Notification.CATEGORY_EVENT -> ActivityType.TIMER
            n.category == Notification.CATEGORY_SERVICE &&
                (title.contains("Recording", true) || text.contains("Recording", true)) ->
                ActivityType.RECORDING
            else -> ActivityType.NOTIFICATION
        }

        val key = keyFor(sbn)
        val progressFraction =
            if (progressMax > 0 && progress >= 0) progress.toFloat() / progressMax.toFloat()
            else -1f

        AetherRuntime.registry.notifications.add(
            AetherNotification(sbn.id, sbn.packageName, title, text, sbn.postTime)
        )
        NotificationReplyBridge.remember(sbn)

        val priority = when (type) {
            ActivityType.CALL -> 90
            ActivityType.RECORDING -> 80
            ActivityType.MEDIA -> 40
            ActivityType.DOWNLOAD -> 35
            ActivityType.TIMER -> 30
            else -> 20
        }

        // Only override if higher or equal priority, or same key
        val current = AetherRuntime.registry.island.activity
        if (current.key == key || priority >= current.priority || current.type == ActivityType.NONE) {
            AetherRuntime.registry.island.setActivity(
                IslandActivity(
                    type = type,
                    title = title.ifBlank { packageLabel(sbn.packageName) },
                    detail = text.ifBlank { sub },
                    progress = progressFraction,
                    priority = priority,
                    key = key,
                    packageName = sbn.packageName,
                    actions = if (NotificationReplyBridge.canReply(key))
                        listOf("Reply", "Mark as read") else emptyList()
                )
            )
        }

        AetherOverlayService.refreshFromSystem()
    }

    private fun isMediaPackage(pkg: String): Boolean {
        val mediaHints = listOf(
            "spotify", "youtube", "music", "podcast", "soundcloud",
            "deezer", "tidal", "apple.android.music", "com.google.android.apps.youtube"
        )
        return mediaHints.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun packageLabel(pkg: String): String =
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("Activity")

    private fun keyFor(sbn: StatusBarNotification) = "${sbn.packageName}:${sbn.id}"
    private fun keyFor(id: Int, packageName: String) = "$packageName:$id"
}
