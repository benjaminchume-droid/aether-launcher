package com.aether.launcher.engine.island

import com.aether.launcher.engine.*

enum class ActivityType {
    NONE,
    NOTIFICATION,
    MEDIA,
    CALL,
    TIMER,
    RECORDING,
    NAVIGATION
}

data class IslandActivity(
    val type: ActivityType,
    val title: String,
    val detail: String = "",
    val priority: Int = 0
)

data class IslandGeometry(
    val widthDp: Float = 92f,
    val heightDp: Float = 30f,
    val radiusDp: Float = 18f
)

class DynamicIslandEngine : AetherEngine {
    override val id: String = "dynamic-island"

    private var state: EngineHealth = EngineHealth.STOPPED

    private var _geometry: IslandGeometry = IslandGeometry()
    val geometry: IslandGeometry
        get() = _geometry

    private var _activity: IslandActivity = IslandActivity(ActivityType.NONE, "")
    val activity: IslandActivity
        get() = _activity

    override fun start() {
        state = EngineHealth.RUNNING
    }

    override fun stop() {
        state = EngineHealth.STOPPED
    }

    override fun health(): EngineHealth = state

    fun setActivity(value: IslandActivity) {
        _activity = value
    }

    fun setGeometry(value: IslandGeometry) {
        _geometry = value
    }
}
