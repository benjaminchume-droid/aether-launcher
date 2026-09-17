package com.aether.launcher.system

import android.animation.ValueAnimator
import android.graphics.*
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
import com.aether.launcher.ui.GlassPainter
import kotlin.math.max
import kotlin.math.min

/** Secondary island renderer (kept in sync with NotificationIslandOverlay styles). */
class AetherIslandOverlay(private val service: AetherOverlayService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val store = AetherSettingsStore(service)
    private var view: IslandView? = null
    private var params: WindowManager.LayoutParams? = null
    private var targetWidth = 0
    private var targetHeight = 0
    private var expanded = false
    private var boundsAnimator: ValueAnimator? = null
    private val density get() = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    fun show() {
        if (view != null || !Settings.canDrawOverlays(service)) return
        val cfg = store.load().island
        val island = IslandView(service)
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(
            dp(cfg.width.coerceAtLeast(86)),
            dp(cfg.height.coerceAtLeast(26)),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
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
            island.scaleX = 0.82f
            island.scaleY = 0.82f
            SpringAnimation(island, DynamicAnimation.SCALE_X).apply { spring = spring(1f, 680f); start() }
            SpringAnimation(island, DynamicAnimation.SCALE_Y).apply { spring = spring(1f, 680f); start() }
            refresh()
        }
    }

    private fun spring(end: Float, stiffness: Float) =
        SpringForce(end).apply { this.stiffness = stiffness; dampingRatio = 0.74f }

    private fun position(p: WindowManager.LayoutParams, offsetDp: Int) {
        if (Build.VERSION.SDK_INT >= 30) {
            val insets = wm.currentWindowMetrics.windowInsets
            val topInset = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            val cutout = insets.displayCutout?.boundingRects?.maxByOrNull { it.width() * it.height() }
            if (cutout != null && store.load().island.cutoutAware) {
                p.y = max(topInset, cutout.bottom) + dp(3)
                p.width = max(p.width, cutout.width() + dp(54))
            } else p.y = topInset + dp(offsetDp)
        } else {
            val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
            val status = if (id != 0) service.resources.getDimensionPixelSize(id) else dp(24)
            p.y = status + dp(offsetDp)
        }
    }

    fun refresh() {
        val v = view ?: return
        val activity = AetherRuntime.registry.island.activity
        v.activity = activity
        val cfg = store.load().island
        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()
        val desiredWidth = if (expanded && active) dp(min(380, max(cfg.width + 104, 258))) else dp(cfg.width.coerceAtLeast(86))
        val desiredHeight = if (expanded && active) dp(max(cfg.height + 54, 82)) else dp(cfg.height.coerceAtLeast(26))
        animateBounds(desiredWidth, desiredHeight)
        v.invalidate()
    }

    private fun animateBounds(width: Int, height: Int) {
        val p = params ?: return
        val v = view ?: return
        if (width == targetWidth && height == targetHeight) return
        boundsAnimator?.cancel()
        val oldW = targetWidth
        val oldH = targetHeight
        targetWidth = width
        targetHeight = height
        boundsAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            addUpdateListener { value ->
                val t = value.animatedValue as Float
                val eased = 1f - (1f - t) * (1f - t)
                p.width = (oldW + (width - oldW) * eased).toInt()
                p.height = (oldH + (height - oldH) * eased).toInt()
                runCatching {
                    position(p, store.load().island.topOffset)
                    wm.updateViewLayout(v, p)
                }
                v.invalidate()
            }
            start()
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        refresh()
        view?.let {
            it.scaleX = 0.96f
            it.scaleY = 0.96f
            SpringAnimation(it, DynamicAnimation.SCALE_X).apply { spring = spring(1f, 520f); start() }
            SpringAnimation(it, DynamicAnimation.SCALE_Y).apply { spring = spring(1f, 520f); start() }
        }
    }

    fun hide() {
        boundsAnimator?.cancel()
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        params = null
    }
}

private class IslandView(context: android.content.Context) : View(context) {
    var activity = IslandActivity(ActivityType.NONE, "")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val expanded = h > 55f * d
        val radius = min(h / 2f, 28f * d)
        val bounds = RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f)

        GlassPainter.drawGlass(c, bounds, radius, 0xF0121418.toInt())

        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()
        val glyph = when (activity.type) {
            ActivityType.MEDIA -> "▶"
            ActivityType.CALL -> "☎"
            ActivityType.TIMER -> "◷"
            ActivityType.RECORDING -> "●"
            ActivityType.NAVIGATION -> "⌖"
            ActivityType.NOTIFICATION -> "•"
            ActivityType.CHARGING -> "⚡"
            ActivityType.DOWNLOAD -> "⬇"
            ActivityType.SYSTEM -> "⚙"
            ActivityType.NONE -> ""
        }

        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = if (expanded) 15f * d else 12f * d
        paint.color = when (activity.type) {
            ActivityType.RECORDING -> 0xFFFF5D67.toInt()
            ActivityType.CALL -> 0xFF77E88E.toInt()
            ActivityType.NAVIGATION -> 0xFF7FC8FF.toInt()
            ActivityType.CHARGING -> 0xFF8BE28A.toInt()
            ActivityType.DOWNLOAD -> 0xFF69F0AE.toInt()
            ActivityType.MEDIA -> 0xFFEA80FC.toInt()
            else -> Color.WHITE
        }
        if (glyph.isNotEmpty()) c.drawText(glyph, 16f * d, min(h * 0.54f, 23f * d), paint)

        val titleX = if (glyph.isNotEmpty()) 38f * d else w / 2f
        paint.color = 0xF4FFFFFF.toInt()
        paint.textSize = if (expanded) 12f * d else 10f * d
        paint.textAlign = if (glyph.isNotEmpty()) Paint.Align.LEFT else Paint.Align.CENTER
        c.drawText(
            if (active) activity.title.take(if (expanded) 36 else 24) else "AETHER",
            titleX, min(h * 0.54f, 23f * d), paint
        )

        if (!expanded) return

        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f * d
        paint.color = 0xBFFFFFFF.toInt()
        if (activity.detail.isNotBlank()) c.drawText(activity.detail.take(48), 38f * d, 42f * d, paint)

        when (activity.type) {
            ActivityType.MEDIA -> drawMedia(c, d, w)
            ActivityType.CALL -> drawCall(c, d, w)
            ActivityType.TIMER -> drawTimer(c, d, w)
            ActivityType.RECORDING -> drawRecording(c, d)
            ActivityType.NAVIGATION -> drawNavigation(c, d, w)
            ActivityType.CHARGING -> drawCharging(c, d, w)
            ActivityType.DOWNLOAD -> drawDownload(c, d, w)
            else -> drawNotification(c, d)
        }
    }

    private fun drawMedia(c: Canvas, d: Float, w: Float) {
        paint.color = 0x22FFFFFF
        c.drawRoundRect(RectF(38f * d, 52f * d, w - 38f * d, 56f * d), 3f * d, 3f * d, paint)
        if (activity.progress in 0f..1f) {
            paint.color = 0xFFEA80FC.toInt()
            val filled = 38f * d + (w - 76f * d) * activity.progress
            c.drawRoundRect(RectF(38f * d, 52f * d, filled, 56f * d), 3f * d, 3f * d, paint)
        }
        paint.color = Color.WHITE
        paint.textSize = 18f * d
        paint.textAlign = Paint.Align.CENTER
        c.drawText("‹    ❚❚    ›", w / 2f, 74f * d, paint)
    }

    private fun drawCall(c: Canvas, d: Float, w: Float) {
        paint.color = 0xFFFF5252.toInt()
        c.drawCircle(w - 78f * d, 36f * d, 14f * d, paint)
        paint.color = 0xFF448AFF.toInt()
        c.drawCircle(w - 36f * d, 36f * d, 14f * d, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f * d
        paint.textAlign = Paint.Align.CENTER
        c.drawText("☎", w - 78f * d, 41f * d, paint)
        c.drawText("☎", w - 36f * d, 41f * d, paint)
    }

    private fun drawTimer(c: Canvas, d: Float, w: Float) {
        paint.color = Color.WHITE
        paint.textSize = 14f * d
        paint.textAlign = Paint.Align.RIGHT
        c.drawText("Running", w - 18f * d, 74f * d, paint)
    }

    private fun drawRecording(c: Canvas, d: Float) {
        paint.color = 0xFFFF5D67.toInt()
        c.drawCircle(48f * d, 72f * d, 5f * d, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f * d
        paint.textAlign = Paint.Align.LEFT
        c.drawText("Recording", 60f * d, 76f * d, paint)
    }

    private fun drawNavigation(c: Canvas, d: Float, w: Float) {
        paint.color = Color.WHITE
        paint.textSize = 11f * d
        paint.textAlign = Paint.Align.RIGHT
        c.drawText("›", w - 20f * d, 74f * d, paint)
    }

    private fun drawCharging(c: Canvas, d: Float, w: Float) {
        paint.color = 0xFF8BE28A.toInt()
        c.drawRoundRect(RectF(38f * d, 61f * d, w - 38f * d, 66f * d), 3f * d, 3f * d, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f * d
        paint.textAlign = Paint.Align.LEFT
        c.drawText("Charging", 38f * d, 79f * d, paint)
    }

    private fun drawDownload(c: Canvas, d: Float, w: Float) {
        paint.color = 0x33FFFFFF
        c.drawRoundRect(RectF(38f * d, 52f * d, w - 38f * d, 58f * d), 3f * d, 3f * d, paint)
        if (activity.progress in 0f..1f) {
            paint.color = 0xFF69F0AE.toInt()
            val filled = 38f * d + (w - 76f * d) * activity.progress
            c.drawRoundRect(RectF(38f * d, 52f * d, filled, 58f * d), 3f * d, 3f * d, paint)
        }
        paint.color = Color.WHITE
        paint.textSize = 10f * d
        paint.textAlign = Paint.Align.LEFT
        c.drawText(activity.detail.ifBlank { "Downloading…" }, 38f * d, 76f * d, paint)
    }

    private fun drawNotification(c: Canvas, d: Float) {
        paint.color = 0xBFFFFFFF.toInt()
        paint.textSize = 10f * d
        paint.textAlign = Paint.Align.LEFT
        c.drawText("Tap to open", 38f * d, 74f * d, paint)
    }
}
