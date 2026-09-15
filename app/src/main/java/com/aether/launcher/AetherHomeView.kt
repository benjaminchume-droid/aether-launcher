package com.aether.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AetherHomeView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val store = AetherSettingsStore(context)
    private val history = AetherHistoryStore(context)
    private val dock = AetherDockStore(context)
    private var cfg = store.load()
    private val apps get() = AetherRuntime.registry.launcher.apps()
    private val density get() = resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private val quickSpring = SpringAnimation(this, DynamicAnimation.TRANSLATION_X)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        quickSpring.spring = SpringForce(0f).apply {
            stiffness = 620f
            dampingRatio = 0.82f
        }
        scaleX = 0.985f
        scaleY = 0.985f
        SpringAnimation(this, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce(1f).apply { stiffness = 520f; dampingRatio = 0.82f }
        }.start()
        SpringAnimation(this, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(1f).apply { stiffness = 520f; dampingRatio = 0.82f }
        }.start()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        cfg = store.load()
        val w = width.toFloat()
        val h = height.toFloat()
        val d = density

        // The wallpaper is painted by AetherGlassRoot underneath this view.
        // Keep this layer translucent so the wallpaper remains the primary visual.
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            0x2A05070B, 0x1405070B,
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        if (cfg.appearance.showTime) {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = 16f * d
            paint.color = 0xE6FFFFFF.toInt()
            c.drawText(timeText("HH:mm"), w / 2f, 34f * d, paint)
        }

        if (cfg.appearance.showAetherLabel) {
            paint.textSize = 8f * d
            paint.letterSpacing = 0.30f
            paint.color = 0xA6FFFFFF.toInt()
            c.drawText("AETHER", w / 2f, 51f * d, paint)
            paint.letterSpacing = 0f
        }

        drawIsland(c, w, d)
        drawHomeGrid(c, w, h, d)
        drawDock(c, w, h, d)
        drawQuickHandle(c, w, h, d)
    }

    private fun drawIsland(c: Canvas, w: Float, d: Float) {
        if (!cfg.island.enabled) return
        val active = AetherRuntime.registry.island.activity
        val baseWidth = min(w * 0.46f, cfg.island.width * d)
        val activeWidth = if (active.title.isNotBlank()) min(w * 0.76f, baseWidth * 1.42f) else baseWidth
        val height = max(26f * d, cfg.island.height * d)
        val y = cfg.island.topOffset * d
        val radius = min(height / 2f, cfg.island.radius * d)
        val rect = RectF(w / 2f - activeWidth / 2f, y, w / 2f + activeWidth / 2f, y + height)

        drawGlass(c, rect, radius, 0xE0000000.toInt())
        paint.color = 0x2AFFFFFF
        c.drawRoundRect(RectF(rect.left + 1f, rect.top + 1f, rect.right - 1f, rect.bottom - 1f), radius, radius, paint)

        // Camera / punch-hole treatment: a darker center gives the capsule a real cutout feel.
        val cutout = min(22f * d, height * 0.62f)
        if (cfg.island.cutoutAware) {
            paint.color = Color.BLACK
            c.drawRoundRect(
                RectF(
                    w / 2f - cutout,
                    y + (height - cutout * 0.52f) / 2f,
                    w / 2f + cutout,
                    y + (height + cutout * 0.52f) / 2f
                ),
                cutout,
                cutout,
                paint
            )
        }

        if (active.title.isNotBlank()) {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 10f * d
            paint.color = Color.WHITE
            c.drawText(active.title.take(28), w / 2f, y + height * 0.62f, paint)
            paint.typeface = Typeface.DEFAULT
        }
    }

    private fun drawHomeGrid(c: Canvas, w: Float, h: Float, d: Float) {
        val columns = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val shown = when (cfg.homeMode) {
            HomeMode.ALL_APPS -> apps.take(columns * rows)
            HomeMode.HOME_AND_DRAWER -> apps.take(columns * min(rows, 3))
            HomeMode.DRAWER_ONLY -> emptyList()
        }
        if (shown.isEmpty()) return

        val left = cfg.grid.horizontalPadding * d
        val right = cfg.grid.horizontalPadding * d
        val horizontalGap = cfg.grid.horizontalSpacing * d
        val usableWidth = max(1f, w - left - right - horizontalGap * (columns - 1))
        val cell = usableWidth / columns

        val dockReserve = if (cfg.dock.enabled) cfg.dock.height * d + cfg.dock.bottomPadding * d + 34f * d else 18f * d
        val top = max(78f * d, 92f * d + cfg.grid.verticalPadding * d)
        val availableHeight = max(1f, h - top - dockReserve)
        val verticalGap = cfg.grid.verticalSpacing * d
        val rowCell = max(1f, (availableHeight - verticalGap * (rows - 1)) / rows)
        val icon = min(
            min(cfg.grid.iconSize * d, cell * 0.70f),
            rowCell * 0.66f
        ).coerceAtLeast(22f * d)

        shown.forEachIndexed { index, app ->
            val col = index % columns
            val row = index / columns
            if (row >= rows) return@forEachIndexed
            val x = left + col * (cell + horizontalGap)
            val y = top + row * (rowCell + verticalGap)
            val cx = x + cell / 2f
            val iconTop = y + max(2f * d, (rowCell - icon) * 0.18f)
            val iconRect = RectF(cx - icon / 2f, iconTop, cx + icon / 2f, iconTop + icon)
            drawIcon(c, AetherRuntime.registry.launcher.icon(app.packageName), iconRect)

            if (cfg.grid.showLabels) {
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                paint.textSize = min(cfg.grid.labelSize * d, max(8f * d, rowCell * 0.12f))
                paint.color = 0xE6FFFFFF.toInt()
                c.drawText(app.label.take(12), cx, iconRect.bottom + min(18f * d, rowCell * 0.20f), paint)
            }
        }
    }

    private fun drawDock(c: Canvas, w: Float, h: Float, d: Float) {
        if (!cfg.dock.enabled) return
        val dockHeight = cfg.dock.height * d
        val bottom = h - cfg.dock.bottomPadding * d
        val top = bottom - dockHeight
        val rect = RectF(w * 0.045f, top, w * 0.955f, bottom)
        drawGlass(c, rect, cfg.dock.radius * d, 0xBE15191F.toInt())

        val favoriteApps = dock.favorites()
            .mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .distinctBy { it.packageName }
        val chosen = (favoriteApps + apps.filterNot { app -> favoriteApps.any { it.packageName == app.packageName } })
            .take(cfg.dock.appCount.coerceIn(3, 8))
        if (chosen.isEmpty()) return

        val count = chosen.size
        val slot = rect.width() / count
        val iconSize = min(54f * d, dockHeight * 0.64f)
        chosen.forEachIndexed { index, app ->
            val centerX = rect.left + slot * (index + 0.5f)
            val r = RectF(
                centerX - iconSize / 2f,
                top + (dockHeight - iconSize) / 2f,
                centerX + iconSize / 2f,
                top + (dockHeight + iconSize) / 2f
            )
            drawIcon(c, AetherRuntime.registry.launcher.icon(app.packageName), r)
        }
    }

    private fun drawQuickHandle(c: Canvas, w: Float, h: Float, d: Float) {
        if (!cfg.quickSpace.enabled) return
        val thickness = cfg.quickSpace.handleThickness * d
        val length = cfg.quickSpace.handleLength * d
        val centerY = h * 0.52f
        val right = cfg.quickSpace.edge == QuickEdge.RIGHT
        val rect = if (right) {
            RectF(w - 6f * d - thickness, centerY - length / 2f, w - 6f * d, centerY + length / 2f)
        } else {
            RectF(6f * d, centerY - length / 2f, 6f * d + thickness, centerY + length / 2f)
        }
        paint.color = 0xDFFFFFFF.toInt()
        c.drawRoundRect(rect, thickness, thickness, paint)
        paint.color = 0x35FFFFFF
        c.drawRoundRect(RectF(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.42f), thickness, thickness, paint)
    }

    private fun drawGlass(c: Canvas, rect: RectF, radius: Float, base: Int) {
        paint.shader = null
        paint.setShadowLayer(cfg.glass.depth * density, 0f, 5f * density, 0x76000000)
        paint.color = base
        c.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()

        paint.shader = LinearGradient(
            0f,
            rect.top,
            0f,
            rect.bottom,
            0x38FFFFFF,
            0x10FFFFFF,
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(RectF(rect.left + 0.7f, rect.top + 0.7f, rect.right - 0.7f, rect.bottom - 0.7f), radius, radius, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, density)
        paint.color = 0x48FFFFFF.toInt()
        c.drawRoundRect(RectF(rect.left + 1f, rect.top + 1f, rect.right - 1f, rect.bottom - 1f), radius, radius, paint)
        paint.style = Paint.Style.FILL

        paint.shader = LinearGradient(
            0f,
            rect.top,
            0f,
            rect.top + rect.height() * 0.22f,
            0x36FFFFFF,
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
    }

    private fun drawIcon(c: Canvas, icon: Drawable?, r: RectF) {
        icon ?: return
        icon.setBounds(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
        icon.draw(c)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val d = density

                if (cfg.quickSpace.enabled &&
                    abs(dx) > cfg.quickSpace.triggerDistance * d &&
                    ((cfg.quickSpace.edge == QuickEdge.RIGHT && downX > width - 110f * d) ||
                        (cfg.quickSpace.edge == QuickEdge.LEFT && downX < 110f * d))
                ) {
                    quickSpring.animateToFinalPosition(if (cfg.quickSpace.edge == QuickEdge.RIGHT) -22f * d else 22f * d)
                    start(AetherSurface.QUICK)
                    return true
                }

                if (abs(dy) > 90f * d && dy < 0f && downY > 80f * d) {
                    start(AetherSurface.DRAWER)
                    return true
                }
                if (abs(dy) > 90f * d && dy > 0f && downY < height * 0.48f) {
                    start(AetherSurface.WIDGETS)
                    return true
                }
                if (downY > height * 0.79f && abs(dx) < 30f * d && abs(dy) < 30f * d) {
                    start(AetherSurface.MULTITASK)
                    return true
                }
                if (abs(dx) < 30f * d && abs(dy) < 30f * d) {
                    launchApp(event.x, event.y)
                    return true
                }
                return true
            }
        }
        return true
    }

    private fun launchApp(x: Float, y: Float) {
        val d = density
        val columns = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val shown = when (cfg.homeMode) {
            HomeMode.ALL_APPS -> apps.take(columns * rows)
            HomeMode.HOME_AND_DRAWER -> apps.take(columns * min(rows, 3))
            HomeMode.DRAWER_ONLY -> emptyList()
        }
        if (shown.isEmpty()) return

        val left = cfg.grid.horizontalPadding * d
        val horizontalGap = cfg.grid.horizontalSpacing * d
        val cell = (width - left * 2f - horizontalGap * (columns - 1)) / columns
        val dockReserve = if (cfg.dock.enabled) cfg.dock.height * d + cfg.dock.bottomPadding * d + 34f * d else 18f * d
        val top = max(78f * d, 92f * d + cfg.grid.verticalPadding * d)
        val available = max(1f, height - top - dockReserve)
        val verticalGap = cfg.grid.verticalSpacing * d
        val rowCell = max(1f, (available - verticalGap * (rows - 1)) / rows)
        val col = ((x - left) / (cell + horizontalGap)).toInt()
        val row = ((y - top) / (rowCell + verticalGap)).toInt()
        val index = row * columns + col

        if (col in 0 until columns && row in 0 until rows && index in shown.indices) {
            history.record(shown[index].packageName)
            AetherRuntime.registry.launcher.launchIntent(shown[index].packageName)?.let { intent ->
                context.startActivity(intent)
            }
        }
    }

    private fun start(surface: String) {
        context.startActivity(
            Intent(context, AetherSurfaceActivity::class.java)
                .putExtra("surface", surface)
        )
    }

    private fun timeText(pattern: String): String =
        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date())
}
