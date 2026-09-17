package com.aether.launcher.engine.island

import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

enum class ActivityType {
    NONE,
    NOTIFICATION,
    MEDIA,
    CALL,
    TIMER,
    RECORDING,
    NAVIGATION,
    CHARGING,
    DOWNLOAD,
    SYSTEM
}

data class IslandActivity(
    val type: ActivityType,
    val title: String,
    val detail: String = "",
    val progress: Float = -1f,          // 0..1 for download / media
    val priority: Int = 0,
    val key: String = "",
    val packageName: String = "",
    val actions: List<String> = emptyList() // e.g. ["Reply", "Mark as read"]
)

data class IslandGeometry(
    val widthDp: Float = 92f,
    val heightDp: Float = 30f,
    val radiusDp: Float = 18f,
    val expandedWidthDp: Float = 280f,
    val expandedHeightDp: Float = 84f
)

/**
 * Central state for the Dynamic Island / Capsule.
 * Driven only by real sources (NotificationListener, media session, call state, etc.).
 */
class DynamicIslandEngine : AetherEngine {
    override val id = "dynamic-island"
    private var state = EngineHealth.STOPPED
    private var _geometry = IslandGeometry()
    private var _activity = IslandActivity(ActivityType.NONE, "")
    private val listeners = mutableListOf<(IslandActivity) -> Unit>()

    val geometry get() = _geometry
    val activity get() = _activity

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() {
        state = EngineHealth.STOPPED
        listeners.clear()
        _activity = IslandActivity(ActivityType.NONE, "")
    }
    override fun health() = state

    @Synchronized
    fun setActivity(value: IslandActivity) {
        _activity = value
        listeners.toList().forEach { listener ->
            runCatching { listener(value) }
        }
    }

    @Synchronized
    fun clearActivity(key: String? = null) {
        if (key == null || _activity.key == key) {
            setActivity(IslandActivity(ActivityType.NONE, ""))
        }
    }

    @Synchronized
    fun observe(listener: (IslandActivity) -> Unit): () -> Unit {
        listeners += listener
        listener(_activity)
        return { synchronized(this) { listeners.remove(listener) } }
    }

    fun setGeometry(value: IslandGeometry) {
        _geometry = value
    }

    // Convenience factories matching the reference capsule styles

    fun showMedia(title: String, artist: String, progress: Float = -1f, key: String = "media") {
        setActivity(
            IslandActivity(
                type = ActivityType.MEDIA,
                title = title,
                detail = artist,
                progress = progress,
                priority = 40,
                key = key,
                actions = listOf("prev", "playpause", "next")
            )
        )
    }

    fun showCall(name: String, detail: String = "Incoming Call", key: String = "call") {
        setActivity(
            IslandActivity(
                type = ActivityType.CALL,
                title = name,
                detail = detail,
                priority = 90,
                key = key,
                actions = listOf("decline", "answer")
            )
        )
    }

    fun showDownload(title: String, progress: Float, key: String = "download") {
        setActivity(
            IslandActivity(
                type = ActivityType.DOWNLOAD,
                title = title,
                detail = "${(progress * 100).toInt()}%",
                progress = progress.coerceIn(0f, 1f),
                priority = 30,
                key = key
            )
        )
    }

    fun showNotification(
        title: String,
        text: String,
        key: String,
        packageName: String = "",
        canReply: Boolean = false
    ) {
        setActivity(
            IslandActivity(
                type = ActivityType.NOTIFICATION,
                title = title,
                detail = text,
                priority = 50,
                key = key,
                packageName = packageName,
                actions = if (canReply) listOf("Reply", "Mark as read") else listOf("Open")
            )
        )
    }

    fun showRecording(title: String = "Screen Recording", key: String = "recording") {
        setActivity(
            IslandActivity(
                type = ActivityType.RECORDING,
                title = title,
                detail = "Recording",
                priority = 80,
                key = key
            )
        )
    }

    fun showSystem(title: String, detail: String = "", key: String = "system") {
        setActivity(
            IslandActivity(
                type = ActivityType.SYSTEM,
                title = title,
                detail = detail,
                priority = 20,
                key = key
            )
        )
    }
}
