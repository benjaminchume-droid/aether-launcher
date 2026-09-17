package com.aether.launcher.ui

import android.graphics.*
import android.os.Build
import android.view.View
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.glass.GlassMaterial
import kotlin.math.max
import kotlin.math.min

/**
 * Shared liquid-glass painter used by Home, Island, Quick Space, Settings, Lock, etc.
 * Includes refraction-style lighting (top highlight + edge rim + soft depth).
 */
object GlassPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun material(): GlassMaterial =
        if (AetherRuntime.isInitialized()) AetherRuntime.registry.glass.material()
        else GlassMaterial()

    fun applyRealBlur(view: View, radius: Float = material().blurRadius) {
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

    /**
     * Draw a liquid glass surface with refraction cues.
     * @param baseColor ARGB base fill
     * @param radius corner radius in px
     */
    fun drawGlass(
        c: Canvas,
        bounds: RectF,
        radius: Float,
        baseColor: Int = 0xB0121822.toInt(),
        depth: Float = material().elevation,
        highlight: Float = material().highlight
    ) {
        val d = depth.coerceAtLeast(2f)

        // Soft shadow / depth
        paint.style = Paint.Style.FILL
        paint.color = baseColor
        paint.setShadowLayer(d * 1.6f, 0f, d * 0.7f, 0x66000000)
        c.drawRoundRect(bounds, radius, radius, paint)
        paint.clearShadowLayer()

        // Main body
        paint.color = baseColor
        c.drawRoundRect(bounds, radius, radius, paint)

        // Top refraction highlight (simulates light bending)
        val highlightAlpha = (highlight * 255).toInt().coerceIn(20, 180)
        paint.shader = LinearGradient(
            0f, bounds.top,
            0f, bounds.top + bounds.height() * 0.55f,
            Color.argb(highlightAlpha, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(
            RectF(bounds.left + 1f, bounds.top + 1f, bounds.right - 1f, bounds.bottom - 1f),
            radius, radius, paint
        )
        paint.shader = null

        // Subtle left-edge refraction (chromatic-ish shift)
        paint.shader = LinearGradient(
            bounds.left, 0f,
            bounds.left + bounds.width() * 0.18f, 0f,
            Color.argb(40, 180, 220, 255),
            Color.argb(0, 180, 220, 255),
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(
            RectF(bounds.left + 0.5f, bounds.top + 0.5f, bounds.right - 0.5f, bounds.bottom - 0.5f),
            radius, radius, paint
        )
        paint.shader = null

        // Rim / border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, radius * 0.04f)
        paint.color = Color.argb((material().borderAlpha * 255).toInt().coerceIn(30, 160), 255, 255, 255)
        c.drawRoundRect(
            RectF(bounds.left + 0.5f, bounds.top + 0.5f, bounds.right - 0.5f, bounds.bottom - 0.5f),
            radius, radius, paint
        )
        paint.style = Paint.Style.FILL
    }

    fun drawPill(
        c: Canvas,
        bounds: RectF,
        baseColor: Int = 0xE010141C.toInt()
    ) {
        val radius = min(bounds.height() / 2f, bounds.width() / 2f)
        drawGlass(c, bounds, radius, baseColor, depth = 6f, highlight = 0.45f)
    }
}
