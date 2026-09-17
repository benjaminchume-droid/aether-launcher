package com.aether.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSearchActivity
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import com.aether.launcher.ui.GlassPainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Wallpaper-first Aether home with liquid glass + refraction on every surface. */
class AetherHomeView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val settings = AetherSettingsStore(context)
    private val layout = AetherHomeLayoutStore(context)
    private val folders = AetherFolderStore(context)
    private val dock = AetherDockStore(context)
    private val history = AetherHistoryStore(context)
    private val handler = Handler(Looper.getMainLooper())
    private val d get() = resources.displayMetrics.density

    private var cfg = settings.load()
    private var apps = mutableListOf<String>()
    private var page = 0
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var dragging: String? = null
    private var dragTarget = -1
    private var pressed: String? = null
    private var scale = 1f
    private var searchLock = false

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        syncApps()
    }

    override fun onDraw(c: Canvas) {
        cfg = settings.load()
        syncApps()
        drawBackground(c)
        drawTop(c)
        // Island is drawn system-wide by OverlayService; we still draw a lightweight indicator here
        drawIslandHint(c)
        drawApps(c)
        drawDock(c)
        drawHandle(c)
        drawDrag(c)
    }

    private fun syncApps() {
        apps = layout.order(AetherRuntime.registry.launcher.apps().map { it.packageName }).toMutableList()
        page = page.coerceIn(0, pageCount() - 1)
    }

    private fun pageSize() = cfg.grid.columns.coerceIn(4, 9) * cfg.grid.rows.coerceIn(4, 9)
    private fun pageCount() = max(1, (apps.size + pageSize() - 1) / pageSize())

    private fun drawBackground(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, 0f, h, 0x1A080B12, 0x3A05070B, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.18f, h * 0.18f, w * 0.72f, 0x353D6E9B, 0x00000000, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.88f, h * 0.70f, w * 0.65f, 0x241C6659, 0x00000000, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    private fun drawTop(c: Canvas) {
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT
        p.textSize = 11f * d
        p.color = 0xBFFFFFFF.toInt()
        c.drawText(
            SimpleDateFormat("EEE  •  d MMM", Locale.getDefault()).format(Date()).uppercase(),
            24f * d, 42f * d, p
        )
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 27f * d
        p.color = Color.WHITE
        c.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f * d, 73f * d, p)
        p.textAlign = Paint.Align.RIGHT
        p.typeface = Typeface.DEFAULT
        p.textSize = 8f * d
        p.letterSpacing = 0.25f
        p.color = 0x80FFFFFF.toInt()
        if (cfg.appearance.showAetherLabel) c.drawText("A E T H E R", width - 24f * d, 43f * d, p)
        p.letterSpacing = 0f
        p.textAlign = Paint.Align.CENTER
    }

    private fun drawIslandHint(c: Canvas) {
        if (!cfg.island.enabled) return
        val a = AetherRuntime.registry.island.activity
        val active = a.type != ActivityType.NONE && a.title.isNotBlank()
        val w = if (active) min(width * 0.72f, 230f * d) else min(width * 0.34f, 132f * d)
        val h = if (active) 42f * d else 34f * d
        val top = max(5f * d, cfg.island.topOffset * d)
        val r = RectF(width / 2f - w / 2f, top, width / 2f + w / 2f, top + h)
        GlassPainter.drawGlass(c, r, h / 2f, if (active) 0xCE171E27.toInt() else 0xAD11161E.toInt())
        p.color = when (a.type) {
            ActivityType.RECORDING -> 0xFFFF5967.toInt()
            ActivityType.CALL -> 0xFF69F0AE.toInt()
            ActivityType.MEDIA -> 0xFFEA80FC.toInt()
            else -> 0xAFFFFFFF.toInt()
        }
        c.drawCircle(r.left + 18f * d, r.centerY(), 3.5f * d, p)
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 10f * d
        p.color = Color.WHITE
        c.drawText(
            if (active) a.title.ifBlank { "Aether Activity" }.take(24) else "AETHER",
            r.left + 28f * d, r.centerY() + 4f * d, p
        )
    }

    private fun drawApps(c: Canvas) {
        val cols = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val left = cfg.grid.horizontalPadding * d
        val gap = cfg.grid.horizontalSpacing * d
        val cell = (width - left * 2f - gap * (cols - 1)) / cols
        val top = 112f * d
        val bottom = height - (if (cfg.dock.enabled) cfg.dock.height * d + cfg.dock.bottomPadding * d + 30f * d else 18f * d)
        val rowGap = cfg.grid.verticalSpacing * d
        val rowH = max(70f * d, (bottom - top - rowGap * (rows - 1)) / rows)
        val icon = cfg.grid.iconSize.coerceIn(44, 70) * d
        val start = page * pageSize()
        val end = min(apps.size, start + pageSize())
        for (i in start until end) {
            val token = apps[i]
            if (token == dragging) continue
            val local = i - start
            val col = local % cols
            val row = local / cols
            val cx = left + col * (cell + gap) + cell / 2f
            val cy = top + row * (rowH + rowGap) + icon * 0.42f
            val s = if (pressed == token) scale else 1f
            val r = RectF(cx - icon * s / 2f, cy - icon * s / 2f, cx + icon * s / 2f, cy + icon * s / 2f)
            if (token.startsWith("folder:")) drawFolder(c, r, token.removePrefix("folder:"))
            else drawIcon(c, AetherRuntime.registry.launcher.icon(token), r)
            if (cfg.grid.showLabels) {
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT
                p.textSize = cfg.grid.labelSize.coerceIn(9, 13) * d
                p.color = 0xF2FFFFFF.toInt()
                c.drawText(label(token).take(14), cx, r.bottom + 15f * d, p)
            }
            if (i == dragTarget && dragging != null) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f * d
                p.color = 0xBFFFFFFF.toInt()
                c.drawRoundRect(RectF(cx - icon * 0.58f, cy - icon * 0.66f, cx + icon * 0.58f, cy + icon * 0.66f), 22f * d, 22f * d, p)
                p.style = Paint.Style.FILL
            }
        }
    }

    private fun drawDock(c: Canvas) {
        if (!cfg.dock.enabled) return
        val bottom = height - cfg.dock.bottomPadding * d
        val top = bottom - cfg.dock.height * d
        val r = RectF(12f * d, top, width - 12f * d, bottom)
        GlassPainter.drawGlass(c, r, cfg.dock.radius * d, 0xA0101722.toInt())
        val items = (dock.favorites() + apps.filterNot { it.startsWith("folder:") })
            .distinct()
            .take(cfg.dock.appCount.coerceIn(4, 6))
        if (items.isEmpty()) return
        val slot = r.width() / items.size
        val size = min(56f * d, r.height() * 0.72f)
        items.forEachIndexed { i, pkg ->
            val x = r.left + slot * (i + 0.5f)
            drawIcon(
                c,
                AetherRuntime.registry.launcher.icon(pkg),
                RectF(x - size / 2f, r.centerY() - size / 2f, x + size / 2f, r.centerY() + size / 2f)
            )
        }
    }

    private fun drawHandle(c: Canvas) {
        if (!cfg.quickSpace.enabled) return
        val x = if (cfg.quickSpace.edge == QuickEdge.RIGHT) width - 5f * d else 5f * d
        val r = RectF(x - 2f * d, height * 0.45f, x + 2f * d, height * 0.45f + cfg.quickSpace.handleLength * d)
        GlassPainter.drawPill(c, r, 0xE0FFFFFF.toInt())
    }

    private fun drawFolder(c: Canvas, r: RectF, id: String) {
        GlassPainter.drawGlass(c, r, r.width() * 0.27f, 0xB61B2530.toInt())
        val pkgs = folders.load().firstOrNull { it.id == id }?.packages.orEmpty().take(4)
        val s = r.width() * 0.29f
        pkgs.forEachIndexed { i, pkg ->
            val x = r.left + r.width() * 0.17f + (i % 2) * (s + r.width() * 0.12f)
            val y = r.top + r.height() * 0.17f + (i / 2) * (s + r.height() * 0.12f)
            drawIcon(c, AetherRuntime.registry.launcher.icon(pkg), RectF(x, y, x + s, y + s))
        }
    }

    private fun drawIcon(c: Canvas, icon: Drawable?, r: RectF) {
        icon ?: return
        icon.setBounds(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
        icon.draw(c)
    }

    private fun label(token: String) =
        if (token.startsWith("folder:"))
            folders.load().firstOrNull { it.id == token.removePrefix("folder:") }?.name ?: "Folder"
        else
            AetherRuntime.registry.launcher.apps().firstOrNull { it.packageName == token }?.label ?: "App"

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y; dragX = e.x; dragY = e.y
                pressed = hit(e.x, e.y)
                if (pressed != null) {
                    handler.postDelayed({
                        val token = pressed
                        if (token != null && dragging == null) startDrag(token)
                    }, 360)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                dragX = e.x; dragY = e.y
                if (dragging != null) {
                    dragTarget = hitIndex(e.x, e.y)
                    invalidate()
                } else if (abs(e.x - downX) > 12f * d || abs(e.y - downY) > 12f * d) {
                    pressed = null
                    handler.removeCallbacksAndMessages(null)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                val dx = e.x - downX
                val dy = e.y - downY
                if (dragging != null) {
                    finishDrag()
                    return true
                }
                pressed = null
                if (dy > 90f * d && abs(dy) > abs(dx) * 1.15f) {
                    if (cfg.homeMode == HomeMode.ALL_APPS && !searchLock) {
                        searchLock = true
                        context.startActivity(Intent(context, AetherSearchActivity::class.java))
                        postDelayed({ searchLock = false }, 500)
                    }
                    return true
                }
                val edge = if (cfg.quickSpace.edge == QuickEdge.RIGHT) downX > width - 100f * d else downX < 100f * d
                if (edge && abs(dx) > cfg.quickSpace.triggerDistance * d && abs(dx) > abs(dy) * 1.1f) {
                    open(AetherSurface.QUICK)
                    return true
                }
                if (abs(dx) > 100f * d && abs(dx) > abs(dy) * 1.2f) {
                    page = (page + if (dx < 0) 1 else -1).coerceIn(0, pageCount() - 1)
                    invalidate()
                    return true
                }
                if (abs(dx) < 24f * d && abs(dy) < 24f * d) {
                    if (islandHit(downX, downY)) {
                        open(AetherSurface.NOTIFICATIONS)
                        return true
                    }
                    hit(e.x, e.y)?.let { launch(it) }
                }
            }
        }
        return true
    }

    private fun islandHit(x: Float, y: Float): Boolean {
        val w = min(width * 0.72f, 230f * d)
        val h = 42f * d
        val top = max(5f * d, cfg.island.topOffset * d)
        return cfg.island.enabled &&
            x in (width / 2f - w / 2f)..(width / 2f + w / 2f) &&
            y in top..(top + h)
    }

    private fun startDrag(token: String) {
        dragging = token
        dragX = downX; dragY = downY
        dragTarget = apps.indexOf(token)
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        animateScale(1.08f)
        invalidate()
    }

    private fun finishDrag() {
        val token = dragging ?: return
        val target = dragTarget
        if (target in apps.indices && target != apps.indexOf(token)) {
            val other = apps[target]
            if (!token.startsWith("folder:") && !other.startsWith("folder:")) {
                merge(token, other)
            } else {
                apps.remove(token)
                apps.add(target.coerceIn(0, apps.size), token)
                layout.save(apps)
            }
        }
        dragging = null
        dragTarget = -1
        animateScale(1f)
        invalidate()
    }

    private fun merge(a: String, b: String) {
        val id = UUID.randomUUID().toString()
        folders.save(AetherFolder(id, "${label(a)} & ${label(b)}", listOf(a, b)))
        val pos = minOf(apps.indexOf(a), apps.indexOf(b))
        apps.removeAll { it == a || it == b }
        apps.add(pos.coerceAtMost(apps.size), "folder:$id")
        layout.save(apps)
    }

    private fun hit(x: Float, y: Float): String? {
        val i = hitIndex(x, y)
        if (i in apps.indices) return apps[i]
        return hitDock(x, y)
    }

    private fun hitIndex(x: Float, y: Float): Int {
        val cols = cfg.grid.columns.coerceIn(4, 9)
        val left = cfg.grid.horizontalPadding * d
        val gap = cfg.grid.horizontalSpacing * d
        val cell = (width - left * 2f - gap * (cols - 1)) / cols
        val top = 112f * d
        val bottom = height - (if (cfg.dock.enabled) cfg.dock.height * d + cfg.dock.bottomPadding * d + 30f * d else 18f * d)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val rowGap = cfg.grid.verticalSpacing * d
        val rowH = max(70f * d, (bottom - top - rowGap * (rows - 1)) / rows)
        val col = ((x - left) / (cell + gap)).toInt()
        val row = ((y - top) / (rowH + rowGap)).toInt()
        if (col !in 0 until cols || row !in 0 until rows) return -1
        val i = page * pageSize() + row * cols + col
        return if (i in apps.indices) i else -1
    }

    private fun hitDock(x: Float, y: Float): String? {
        if (!cfg.dock.enabled) return null
        val bottom = height - cfg.dock.bottomPadding * d
        val top = bottom - cfg.dock.height * d
        if (y !in top..bottom) return null
        val r = RectF(12f * d, top, width - 12f * d, bottom)
        val items = (dock.favorites() + apps.filterNot { it.startsWith("folder:") })
            .distinct()
            .take(cfg.dock.appCount.coerceIn(4, 6))
        if (items.isEmpty()) return null
        return items.getOrNull(((x - r.left) / (r.width() / items.size)).toInt())
    }

    private fun animateScale(target: Float) {
        ValueAnimator.ofFloat(scale, target).apply {
            duration = 180
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener {
                scale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun drawDrag(c: Canvas) {
        val token = dragging ?: return
        val s = cfg.grid.iconSize.coerceIn(48, 72) * d
        val r = RectF(dragX - s / 2f, dragY - s / 2f, dragX + s / 2f, dragY + s / 2f)
        GlassPainter.drawGlass(c, r, 24f * d, 0xD61A2430.toInt())
        if (token.startsWith("folder:"))
            drawFolder(c, RectF(r.left + 7, r.top + 7, r.right - 7, r.bottom - 7), token.removePrefix("folder:"))
        else
            drawIcon(c, AetherRuntime.registry.launcher.icon(token), RectF(r.left + 7, r.top + 7, r.right - 7, r.bottom - 7))
    }

    private fun launch(token: String) {
        if (token.startsWith("folder:")) {
            open(AetherSurface.FOLDER, token.removePrefix("folder:"))
            return
        }
        history.record(token)
        AetherRuntime.registry.launcher.launchIntent(token)?.let { context.startActivity(it) }
    }

    private fun open(surface: String, folderId: String? = null) {
        context.startActivity(Intent(context, AetherSurfaceActivity::class.java).apply {
            putExtra("surface", surface)
            if (folderId != null) putExtra("folder_id", folderId)
        })
    }
}
