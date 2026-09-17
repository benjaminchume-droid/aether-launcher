package com.aether.launcher

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
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

/** Real wallpaper + liquid glass home. Long-press for uninstall / info / remove. */
class AetherHomeView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val settings = AetherSettingsStore(context)
    private val layout = AetherHomeLayoutStore(context)
    private val folders = AetherFolderStore(context)
    private val dockStore = AetherDockStore(context)
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
    private var longPressFired = false

    private val wallpaperDrawable: Drawable? = runCatching {
        WallpaperManager.getInstance(context).drawable
    }.getOrNull()

    private val longPress = Runnable {
        val token = pressed ?: return@Runnable
        longPressFired = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        showAppMenu(token)
    }

    init {
        // HARDWARE so wallpaper + glass paint correctly (software was washing everything to black)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isClickable = true
        syncApps()
    }

    override fun onDraw(c: Canvas) {
        cfg = settings.load()
        syncApps()
        drawBackground(c)
        drawTop(c)
        drawIslandHint(c)
        drawApps(c)
        drawDock(c)
        drawHandle(c)
        drawDrag(c)
    }

    private fun syncApps() {
        val all = AetherRuntime.registry.launcher.apps().map { it.packageName }
        apps = when (cfg.homeMode) {
            HomeMode.DRAWER_ONLY -> mutableListOf() // empty home; drawer has everything
            else -> layout.order(all).toMutableList()
        }
        page = page.coerceIn(0, max(0, pageCount() - 1))
    }

    private fun pageSize() = cfg.grid.columns.coerceIn(4, 9) * cfg.grid.rows.coerceIn(4, 9)
    private fun pageCount() = max(1, (apps.size + pageSize() - 1) / pageSize())

    /** REAL system wallpaper, then soft atmosphere so glass reads. */
    private fun drawBackground(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val wp = wallpaperDrawable
        if (wp != null) {
            wp.setBounds(0, 0, width, height)
            wp.draw(c)
            // Light dim so icons stay readable without killing the wallpaper
            p.shader = null
            p.color = 0x33000000
            c.drawRect(0f, 0f, w, h, p)
        } else {
            p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF1A2332.toInt(), 0xFF0B0F15.toInt(), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, p)
            p.shader = null
        }
    }

    private fun drawTop(c: Canvas) {
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT
        p.textSize = 11f * d
        p.color = 0xE6FFFFFF.toInt()
        p.setShadowLayer(6f * d, 0f, 2f * d, 0x88000000.toInt())
        c.drawText(
            SimpleDateFormat("EEE  •  d MMM", Locale.getDefault()).format(Date()).uppercase(),
            24f * d, 42f * d, p
        )
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 34f * d
        p.color = Color.WHITE
        c.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f * d, 82f * d, p)
        p.clearShadowLayer()
        p.textAlign = Paint.Align.RIGHT
        p.typeface = Typeface.DEFAULT
        p.textSize = 9f * d
        p.letterSpacing = 0.2f
        p.color = 0x99FFFFFF.toInt()
        if (cfg.appearance.showAetherLabel) c.drawText("A E T H E R", width - 24f * d, 43f * d, p)
        p.letterSpacing = 0f
        p.textAlign = Paint.Align.CENTER
    }

    private fun drawIslandHint(c: Canvas) {
        if (!cfg.island.enabled) return
        val a = AetherRuntime.registry.island.activity
        val active = a.type != ActivityType.NONE && a.title.isNotBlank()
        val w = min(width * 0.55f, 200f * d)
        val h = 32f * d
        val left = width / 2f - w / 2f
        val top = max(8f * d, cfg.island.topOffset * d)
        val r = RectF(left, top, left + w, top + h)
        GlassPainter.drawGlass(c, r, h / 2f, if (active) 0xCC1A1F28.toInt() else 0xAA12161E.toInt())
        p.color = Color.WHITE
        p.textSize = 11f * d
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        c.drawText(
            if (active) a.title.take(22) else "AETHER",
            width / 2f, top + h * 0.68f, p
        )
    }

    private fun drawApps(c: Canvas) {
        if (cfg.homeMode == HomeMode.DRAWER_ONLY) {
            p.color = 0xAAFFFFFF.toInt()
            p.textSize = 14f * d
            p.textAlign = Paint.Align.CENTER
            c.drawText("Swipe up for App Drawer", width / 2f, height * 0.45f, p)
            return
        }
        val cols = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val left = 16f * d
        val top = 110f * d
        val bottom = height - 110f * d
        val cellW = (width - left * 2) / cols
        val cellH = (bottom - top) / rows
        val icon = min(cfg.grid.iconSize * d, min(cellW, cellH) * 0.55f)
        val start = page * pageSize()
        val slice = apps.drop(start).take(pageSize())

        slice.forEachIndexed { i, token ->
            if (token == dragging) return@forEachIndexed
            val col = i % cols
            val row = i / cols
            val cx = left + col * cellW + cellW / 2f
            val cy = top + row * cellH + cellH * 0.38f
            val s = if (token == pressed) 0.92f else 1f
            val size = icon * s
            drawAppIcon(c, token, cx, cy, size)
            if (cfg.grid.showLabels) {
                p.color = 0xF0FFFFFF.toInt()
                p.textSize = max(10f * d, cfg.grid.labelSize * d)
                p.typeface = Typeface.DEFAULT
                p.textAlign = Paint.Align.CENTER
                p.setShadowLayer(4f * d, 0f, 1f * d, 0xAA000000.toInt())
                c.drawText(label(token).take(12), cx, cy + size / 2f + 16f * d, p)
                p.clearShadowLayer()
            }
        }
    }

    private fun drawAppIcon(c: Canvas, token: String, cx: Float, cy: Float, size: Float) {
        val half = size / 2f
        val r = RectF(cx - half, cy - half, cx + half, cy + half)
        if (token.startsWith("folder:")) {
            GlassPainter.drawGlass(c, r, size * 0.28f, 0xB01A2430.toInt())
            p.color = Color.WHITE
            p.textSize = size * 0.28f
            p.textAlign = Paint.Align.CENTER
            c.drawText("▣", cx, cy + size * 0.1f, p)
        } else {
            val icon = AetherRuntime.registry.launcher.icon(token)
            if (icon != null) {
                icon.setBounds(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                icon.draw(c)
            } else {
                GlassPainter.drawGlass(c, r, size * 0.28f, 0xA0181C24.toInt())
            }
        }
    }

    private fun drawDock(c: Canvas) {
        if (!cfg.dock.enabled) return
        val dockApps = dockStore.load().ifEmpty {
            AetherRuntime.registry.launcher.apps().take(cfg.dock.appCount).map { it.packageName }
        }.take(cfg.dock.appCount.coerceIn(3, 7))
        val h = cfg.dock.height * d
        val pad = cfg.dock.bottomPadding * d
        val r = RectF(18f * d, height - h - pad, width - 18f * d, height - pad)
        GlassPainter.drawGlass(c, r, cfg.dock.radius * d, 0xB0121822.toInt())
        val cell = r.width() / max(1, dockApps.size)
        val icon = min(48f * d, h * 0.55f)
        dockApps.forEachIndexed { i, pkg ->
            val cx = r.left + cell * i + cell / 2f
            val cy = r.centerY()
            drawAppIcon(c, pkg, cx, cy, icon)
        }
    }

    private fun drawHandle(c: Canvas) {
        // App drawer cue
        val y = height - 8f * d
        p.color = 0x66FFFFFF.toInt()
        c.drawRoundRect(RectF(width / 2f - 18f * d, y - 3f * d, width / 2f + 18f * d, y), 3f * d, 3f * d, p)
    }

    private fun drawDrag(c: Canvas) {
        val token = dragging ?: return
        drawAppIcon(c, token, dragX, dragY, 56f * d * scale)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y
                longPressFired = false
                pressed = hit(e.x, e.y)
                handler.postDelayed(longPress, 420)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging != null) {
                    dragX = e.x; dragY = e.y
                    dragTarget = hitIndex(e.x, e.y)
                    invalidate()
                    return true
                }
                if (abs(e.x - downX) > 12f * d || abs(e.y - downY) > 12f * d) {
                    handler.removeCallbacks(longPress)
                    if (pressed != null && !longPressFired && abs(e.x - downX) + abs(e.y - downY) > 28f * d) {
                        startDrag(pressed!!)
                    }
                    pressed = null
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                val dx = e.x - downX
                val dy = e.y - downY
                if (dragging != null) {
                    finishDrag()
                    pressed = null
                    invalidate()
                    return true
                }
                if (longPressFired) {
                    pressed = null
                    invalidate()
                    return true
                }
                // Swipe up → App Drawer
                if (dy < -80f * d && abs(dy) > abs(dx) * 1.2f) {
                    open(AetherSurface.DRAWER)
                    pressed = null
                    invalidate()
                    return true
                }
                // Swipe down → Search
                if (dy > 80f * d && abs(dy) > abs(dx) * 1.2f && e.y < height * 0.35f) {
                    context.startActivity(Intent(context, AetherSearchActivity::class.java))
                    pressed = null
                    invalidate()
                    return true
                }
                // Edge swipe for Quick Space
                if (dx > 80f * d && downX < 28f * d) {
                    open(AetherSurface.QUICK)
                    pressed = null
                    invalidate()
                    return true
                }
                if (abs(dx) > 100f * d && abs(dx) > abs(dy) * 1.2f) {
                    page = (page + if (dx < 0) 1 else -1).coerceIn(0, pageCount() - 1)
                    pressed = null
                    invalidate()
                    return true
                }
                if (abs(dx) < 24f * d && abs(dy) < 24f * d) {
                    if (islandHit(downX, downY)) {
                        open(AetherSurface.NOTIFICATIONS)
                    } else {
                        hit(e.x, e.y)?.let { launch(it) }
                    }
                }
                pressed = null
                invalidate()
            }
        }
        return true
    }

    private fun showAppMenu(token: String) {
        if (token.startsWith("folder:")) {
            openFolder(token.removePrefix("folder:"))
            return
        }
        val name = label(token)
        val items = arrayOf("Open", "App info", "Uninstall", "Remove from Home", "Add to Dock")
        AlertDialog.Builder(context)
            .setTitle(name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> launch(token)
                    1 -> {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        i.data = Uri.parse("package:$token")
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    }
                    2 -> {
                        val i = Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:$token"))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    }
                    3 -> {
                        apps.remove(token)
                        layout.save(apps)
                        invalidate()
                    }
                    4 -> {
                        val dock = dockStore.load().toMutableList()
                        if (token !in dock) {
                            dock += token
                            dockStore.save(dock.take(7))
                        }
                        invalidate()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun islandHit(x: Float, y: Float): Boolean {
        val w = min(width * 0.55f, 200f * d)
        val h = 32f * d
        val top = max(8f * d, cfg.island.topOffset * d)
        return cfg.island.enabled &&
            x in (width / 2f - w / 2f)..(width / 2f + w / 2f) &&
            y in top..(top + h)
    }

    private fun startDrag(token: String) {
        dragging = token
        dragX = downX; dragY = downY
        dragTarget = apps.indexOf(token)
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
        if (cfg.homeMode == HomeMode.DRAWER_ONLY) return -1
        val cols = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val left = 16f * d
        val top = 110f * d
        val bottom = height - 110f * d
        val cellW = (width - left * 2) / cols
        val cellH = (bottom - top) / rows
        if (x < left || x > width - left || y < top || y > bottom) return -1
        val col = ((x - left) / cellW).toInt().coerceIn(0, cols - 1)
        val row = ((y - top) / cellH).toInt().coerceIn(0, rows - 1)
        val idx = page * pageSize() + row * cols + col
        return if (idx in apps.indices) idx else -1
    }

    private fun hitDock(x: Float, y: Float): String? {
        if (!cfg.dock.enabled) return null
        val dockApps = dockStore.load().ifEmpty {
            AetherRuntime.registry.launcher.apps().take(cfg.dock.appCount).map { it.packageName }
        }.take(cfg.dock.appCount)
        val h = cfg.dock.height * d
        val pad = cfg.dock.bottomPadding * d
        val top = height - h - pad
        if (y < top || y > height - pad) return null
        val cell = (width - 36f * d) / max(1, dockApps.size)
        val idx = ((x - 18f * d) / cell).toInt()
        return dockApps.getOrNull(idx)
    }

    private fun launch(token: String) {
        if (token.startsWith("folder:")) {
            openFolder(token.removePrefix("folder:"))
            return
        }
        history.record(token)
        AetherRuntime.registry.launcher.launch(token)
    }

    private fun openFolder(id: String) {
        context.startActivity(
            Intent(context, AetherSurfaceActivity::class.java)
                .putExtra("surface", AetherSurface.FOLDER)
                .putExtra("folder_id", id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun open(surface: String) {
        context.startActivity(
            Intent(context, AetherSurfaceActivity::class.java)
                .putExtra("surface", surface)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun label(token: String): String {
        if (token.startsWith("folder:")) {
            return folders.load().firstOrNull { it.id == token.removePrefix("folder:") }?.name ?: "Folder"
        }
        return AetherRuntime.registry.launcher.apps().firstOrNull { it.packageName == token }?.label ?: token
    }

    private fun animateScale(end: Float) {
        ValueAnimator.ofFloat(scale, end).apply {
            duration = 180
            interpolator = OvershootInterpolator()
            addUpdateListener {
                scale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
