package com.aether.launcher.engine.physics

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

/**
 * Real spring / velocity physics using AndroidX DynamicAnimation.
 * All UI motion should go through here so timing stays consistent.
 */
data class SpringConfig(
    val stiffness: Float = 420f,
    val dampingRatio: Float = 0.72f,
    val finalPosition: Float = 1f
)

class PhysicsEngine : AetherEngine {
    override val id = "physics"
    private var state = EngineHealth.STOPPED
    private var defaultConfig = SpringConfig()

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED }
    override fun health() = state

    fun updateDefaults(config: SpringConfig) {
        defaultConfig = config
    }

    fun spring(
        view: View,
        property: DynamicAnimation.ViewProperty,
        finalPosition: Float = defaultConfig.finalPosition,
        stiffness: Float = defaultConfig.stiffness,
        dampingRatio: Float = defaultConfig.dampingRatio
    ): SpringAnimation {
        val anim = SpringAnimation(view, property)
        anim.spring = SpringForce(finalPosition).apply {
            this.stiffness = stiffness
            this.dampingRatio = dampingRatio
        }
        return anim
    }

    fun scaleIn(view: View, from: Float = 0.82f, to: Float = 1f) {
        view.scaleX = from
        view.scaleY = from
        spring(view, DynamicAnimation.SCALE_X, to).start()
        spring(view, DynamicAnimation.SCALE_Y, to).start()
    }

    fun bounce(view: View) {
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        spring(view, DynamicAnimation.SCALE_X, 1f, 520f, 0.68f).start()
        spring(view, DynamicAnimation.SCALE_Y, 1f, 520f, 0.68f).start()
    }
}
