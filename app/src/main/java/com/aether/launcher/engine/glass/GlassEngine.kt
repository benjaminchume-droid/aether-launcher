package com.aether.launcher.engine.glass

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

/**
 * Real material system.
 * On API 31+ we use RenderEffect.createBlurEffect for true blur.
 * Below that we fall back to painted glass (still real, just no GPU blur).
 */
data class GlassMaterial(
    val opacity: Float = 0.72f,
    val blurRadius: Float = 28f,
    val saturation: Float = 1.12f,
    val refraction: Float = 0.18f,
    val elevation: Float = 8f,
    val highlight: Float = 0.55f,
    val borderAlpha: Float = 0.35f,
    val cornerRadiusDp: Float = 28f
)

class GlassEngine : AetherEngine {
    override val id = "glass"
    private var state = EngineHealth.STOPPED
    private var material = GlassMaterial()

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED }
    override fun health() = state

    fun material(): GlassMaterial = material

    fun update(material: GlassMaterial) {
        this.material = material
    }

    /** Apply real blur to any View when the device supports it. */
    fun applyBlur(view: View, radius: Float = material.blurRadius) {
        if (Build.VERSION.SDK_INT >= 31) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(
                    radius.coerceIn(4f, 80f),
                    radius.coerceIn(4f, 80f),
                    Shader.TileMode.CLAMP
                )
            )
        }
    }

    fun clearEffect(view: View) {
        if (Build.VERSION.SDK_INT >= 31) {
            view.setRenderEffect(null)
        }
    }

    fun supportsRealBlur(): Boolean = Build.VERSION.SDK_INT >= 31
}
