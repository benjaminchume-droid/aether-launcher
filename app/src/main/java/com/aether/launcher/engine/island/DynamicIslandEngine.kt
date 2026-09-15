package com.aether.launcher.engine.island

import com.aether.launcher.engine.*

enum class ActivityType { NONE, NOTIFICATION, MEDIA, CALL, TIMER, RECORDING, NAVIGATION, CHARGING }

data class IslandActivity(val type: ActivityType, val title: String, val detail: String = "", val priority: Int = 0, val key: String = "")
data class IslandGeometry(val widthDp: Float = 92f, val heightDp: Float = 30f, val radiusDp: Float = 18f)

class DynamicIslandEngine : AetherEngine {
    override val id = "dynamic-island"
    private var state = EngineHealth.STOPPED
    private var _geometry = IslandGeometry()
    private var _activity = IslandActivity(ActivityType.NONE, "")
    private val listeners = mutableListOf<(IslandActivity) -> Unit>()

    val geometry get() = _geometry
    val activity get() = _activity

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED; listeners.clear() }
    override fun health() = state

    @Synchronized fun setActivity(value: IslandActivity) {
        _activity = value
        listeners.toList().forEach { listener -> runCatching { listener(value) } }
    }

    @Synchronized fun clearActivity(key: String? = null) {
        if (key == null || _activity.key == key) setActivity(IslandActivity(ActivityType.NONE, ""))
    }

    @Synchronized fun observe(listener: (IslandActivity) -> Unit): () -> Unit {
        listeners += listener
        listener(_activity)
        return { synchronized(this) { listeners.remove(listener) } }
    }

    fun setGeometry(value: IslandGeometry) { _geometry = value }
}
