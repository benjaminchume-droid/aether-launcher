package com.aether.launcher.system

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.settings.AetherSettingsStore
import kotlin.math.max

class AetherIslandOverlay(private val service: AetherOverlayService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val store = AetherSettingsStore(service)
    private var view: IslandView? = null
    private var params: WindowManager.LayoutParams? = null
    private var targetWidth = 0
    private var targetHeight = 0
    private var expanded = false

    private val density get() = service.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()

    fun show() {
        if (view != null || !Settings.canDrawOverlays(service)) return
        val cfg = store.load().island
        val island = IslandView(service)
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(
            dp(cfg.width.coerceAtLeast(86)),
            dp(cfg.height.coerceAtLeast(26)),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        position(p, cfg.topOffset)
        runCatching {
            wm.addView(island, p)
            view = island
            params = p
            targetWidth = p.width
            targetHeight = p.height
            island.setOnClickListener { toggleExpanded() }
            island.scaleX = .84f
            island.scaleY = .84f
            SpringAnimation(island, DynamicAnimation.SCALE_X).apply { spring = spring(.8f, 1f, 620f); start() }
            SpringAnimation(island, DynamicAnimation.SCALE_Y).apply { spring = spring(.8f, 1f, 620f); start() }
            refresh()
        }
    }

    private fun spring(start: Float, end: Float, stiffness: Float): SpringForce =
        SpringForce(end).apply { this.stiffness = stiffness; dampingRatio = .76f }

    private fun position(p: WindowManager.LayoutParams, offsetDp: Int) {
        if (Build.VERSION.SDK_INT >= 30) {
            val insets = wm.currentWindowMetrics.windowInsets
            val topInset = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            val cutout = insets.displayCutout?.boundingRects?.maxByOrNull { it.width() * it.height() }
            val cutoutBottom = cutout?.bottom ?: 0
            p.y = if (cutout != null && store.load().island.cutoutAware) max(topInset, cutoutBottom) + dp(3) else topInset + dp(offsetDp)
            if (cutout != null && store.load().island.cutoutAware) p.width = max(p.width, cutout.width() + dp(54))
        } else {
            val resourceId = service.resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusHeight = if (resourceId != 0) service.resources.getDimensionPixelSize(resourceId) else dp(24)
            p.y = statusHeight + dp(offsetDp)
        }
    }

    fun refresh() {
        val v = view ?: return
        val activity = AetherRuntime.registry.island.activity
        v.activity = activity
        val cfg = store.load().island
        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()
        val desiredWidth = if (expanded && active) dp(minOf(360, maxOf(cfg.width + 90, 250))) else dp(cfg.width.coerceAtLeast(86))
        val desiredHeight = if (expanded && active) dp(maxOf(cfg.height + 44, 76)) else dp(cfg.height.coerceAtLeast(26))
        animateBounds(desiredWidth, desiredHeight)
        v.invalidate()
    }

    private fun animateBounds(width: Int, height: Int) {
        val p = params ?: return
        val v = view ?: return
        if (width == targetWidth && height == targetHeight) return
        val oldW = targetWidth
        val oldH = targetHeight
        targetWidth = width
        targetHeight = height
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280L
            addUpdateListener { value ->
                val t = value.animatedValue as Float
                p.width = (oldW + (width - oldW) * t).toInt()
                p.height = (oldH + (height - oldH) * t).toInt()
                runCatching { position(p, store.load().island.topOffset); wm.updateViewLayout(v, p) }
            }
            start()
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        refresh()
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        params = null
    }
}

private class IslandView(context: android.content.Context) : View(context) {
    var activity: IslandActivity = IslandActivity(ActivityType.NONE, "")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h / 2f

        paint.shader = LinearGradient(0f, 0f, 0f, h, 0xF9000000.toInt(), 0xEA14161B.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(.5f, .5f, w - .5f, h - .5f), radius, radius, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, d)
        paint.color = 0x50FFFFFF.toInt()
        c.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), radius, radius, paint)
        paint.style = Paint.Style.FILL

        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()
        val glyph = when (activity.type) {
            ActivityType.MEDIA -> "▶"
            ActivityType.CALL -> "☎"
            ActivityType.TIMER -> "◷"
            ActivityType.RECORDING -> "●"
            ActivityType.NAVIGATION -> "⌖"
            ActivityType.NOTIFICATION -> "•"
            ActivityType.NONE -> ""
        }
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = if (active) 14f * d else 10f * d
        paint.color = if (activity.type == ActivityType.RECORDING) 0xFFFF5C5C.toInt() else Color.WHITE
        if (glyph.isNotEmpty()) c.drawText(glyph, 18f * d, h / 2f + 5f * d, paint)

        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = if (active) 11f * d else 10f * d
        paint.color = 0xF2FFFFFF.toInt()
        val x = if (glyph.isNotEmpty()) 38f * d else w / 2f
        val align = if (glyph.isNotEmpty()) Paint.Align.LEFT else Paint.Align.CENTER
        paint.textAlign = align
        c.drawText(if (active) activity.title.take(32) else "AETHER", x, h / 2f + 4f * d, paint)

        if (active && activity.detail.isNotBlank() && h > 55f * d) {
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f * d
            paint.color = 0xB8FFFFFF.toInt()
            paint.textAlign = Paint.Align.LEFT
            c.drawText(activity.detail.take(44), 38f * d, h / 2f + 22f * d, paint)
        }
    }
}
