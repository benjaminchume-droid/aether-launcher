package com.aether.launcher.system

import android.content.Context

/** Presentation policy for Aether's small/expanded notification capsules. */
enum class SmallPopupMode { AUTO_DISAPPEAR, NEVER_DISAPPEAR, DISAPPEAR_AFTER_READ }

data class NotificationPresentation(
    val showDuringDnd: Boolean = false,
    val showDuringGames: Boolean = false,
    val expandSilent: Boolean = true,
    val showProfileBadge: Boolean = false,
    val showNumberBadge: Boolean = true,
    val showArrow: Boolean = false,
    val hideWithoutCover: Boolean = false,
    val smallPopupMode: SmallPopupMode = SmallPopupMode.DISAPPEAR_AFTER_READ,
    val expandedSeconds: Int = 5,
    val swipeToRemove: Boolean = false
)

class NotificationPresentationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_notification_presentation", Context.MODE_PRIVATE)

    @Synchronized fun load(): NotificationPresentation = NotificationPresentation(
        showDuringDnd = prefs.getBoolean("dnd", false),
        showDuringGames = prefs.getBoolean("games", false),
        expandSilent = prefs.getBoolean("silentExpand", true),
        showProfileBadge = prefs.getBoolean("profileBadge", false),
        showNumberBadge = prefs.getBoolean("numberBadge", true),
        showArrow = prefs.getBoolean("arrow", false),
        hideWithoutCover = prefs.getBoolean("hideWithoutCover", false),
        smallPopupMode = enum("smallMode", SmallPopupMode.DISAPPEAR_AFTER_READ),
        expandedSeconds = prefs.getInt("expandedSeconds", 5).coerceIn(1, 30),
        swipeToRemove = prefs.getBoolean("swipeToRemove", false)
    )

    @Synchronized fun save(value: NotificationPresentation) {
        prefs.edit()
            .putBoolean("dnd", value.showDuringDnd)
            .putBoolean("games", value.showDuringGames)
            .putBoolean("silentExpand", value.expandSilent)
            .putBoolean("profileBadge", value.showProfileBadge)
            .putBoolean("numberBadge", value.showNumberBadge)
            .putBoolean("arrow", value.showArrow)
            .putBoolean("hideWithoutCover", value.hideWithoutCover)
            .putString("smallMode", value.smallPopupMode.name)
            .putInt("expandedSeconds", value.expandedSeconds.coerceIn(1, 30))
            .putBoolean("swipeToRemove", value.swipeToRemove)
            .apply()
    }

    fun reset() = save(NotificationPresentation())

    private inline fun <reified E : Enum<E>> enum(key: String, fallback: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, fallback.name) ?: fallback.name) }.getOrDefault(fallback)
}
