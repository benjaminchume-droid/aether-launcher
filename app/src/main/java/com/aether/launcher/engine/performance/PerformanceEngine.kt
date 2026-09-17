package com.aether.launcher.engine.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

enum class PerformanceTier { HIGH, BALANCED, LOW }

data class PerformanceProfile(
    val tier: PerformanceTier = PerformanceTier.BALANCED,
    val blurEnabled: Boolean = true,
    val blurRadius: Float = 28f,
    val maxAnimScale: Float = 1f,
    val reduceShadows: Boolean = false,
    val maxOverlayRefreshMs: Long = 220L
)

/**
 * Scales glass / animation cost by device capability.
 * Real memory class + Android version — no fake metrics.
 */
class PerformanceEngine(private val context: Context? = null) : AetherEngine {

    override val id = "performance"
    private var state = EngineHealth.STOPPED
    private var profile = PerformanceProfile()

    override fun start() {
        profile = detect()
        state = EngineHealth.RUNNING
    }

    override fun stop() { state = EngineHealth.STOPPED }
    override fun health() = state

    fun profile(): PerformanceProfile = profile

    fun detect(): PerformanceProfile {
        val ctx = context ?: return PerformanceProfile()
        val am = ctx.getSystemService(ActivityManager::class.java)
        val memClass = am?.memoryClass ?: 128
        val lowRam = if (Build.VERSION.SDK_INT >= 19) am?.isLowRamDevice == true else false

        return when {
            lowRam || memClass < 128 -> PerformanceProfile(
                tier = PerformanceTier.LOW,
                blurEnabled = false,
                blurRadius = 0f,
                maxAnimScale = 0.7f,
                reduceShadows = true,
                maxOverlayRefreshMs = 400L
            )
            memClass >= 256 && Build.VERSION.SDK_INT >= 31 -> PerformanceProfile(
                tier = PerformanceTier.HIGH,
                blurEnabled = true,
                blurRadius = 32f,
                maxAnimScale = 1f,
                reduceShadows = false,
                maxOverlayRefreshMs = 180L
            )
            else -> PerformanceProfile(
                tier = PerformanceTier.BALANCED,
                blurEnabled = Build.VERSION.SDK_INT >= 31,
                blurRadius = 22f,
                maxAnimScale = 0.9f,
                reduceShadows = false,
                maxOverlayRefreshMs = 250L
            )
        }
    }
}
