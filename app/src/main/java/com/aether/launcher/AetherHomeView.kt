package com.aether.launcher

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
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

/**
 * Home draws TRANSPARENT so FLAG_SHOW_WALLPAPER + GlassRoot show the real wallpaper.
 * All-apps mode lists every launcher app. Long-press = uninstall / info / dock.
 */
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
    private var longPressFired = false

    private val longPress = Runnable {
        val token = pressed ?: return@Runnable
        longPressFired = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        showAppMenu(token)
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isClickable = true
        syncApps()
    }

    override fun onDraw(c: Canvas) {
        cfg = settings.load()
        syncApps()
        // NO solid fill — wallpaper shows through
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
            HomeMode.DRAWER_ONLY -> mutableListOf()
            HomeMode.HOME_AND_DRAWER -> {
                // Home shows a curated / ordered subset; still show all if no layout yet
                val ordered = layout.order(all)
                if (ordered.isEmpty()) all.toMutableList() else ordered.toMutableList()
            }
            HomeMode.ALL_APPS -> layout.order(all).toMutableList().ifEmpty { all.toMutableList() }
        }
        // ALWAYS ensure every installed launcher app is present in ALL_APPS mode
        if (cfg.homeMode == HomeMode.ALL_APPS) {
            val missing = all.filter { it !in apps }
            if (missing.isNotEmpty()) {
                apps.addAll(missing)
                layout.save(apps)
            }
        }
        page = page.coerceIn(0, max(0, pageCount() - 1))
    }

    private fun pageSize() = cfg.grid.columns.coerceIn(4, 9) * cfg.grid.rows.coerceIn(4, 9)
    private fun pageCount() = max(1, (apps.size + pageSize() - 1) / pageSize())

    private fun drawTop(c: Canvas) {
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT
        p.textSize = 11f * d
        p.color = 0xF0FFFFFF.toInt()
        p.setShadowLayer(8f * d, 0f, 2f * d, 0xCC000000.toInt())
        c.drawText(
            SimpleDateFormat("EEE  •  d MMM", Locale.getDefault()).format(Date()).uppercase(),
            24f * d, 48f * d, p
        )
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 36f * d
        p.color = Color.WHITE
        c.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f * d, 90f * d, p)
        p.clearShadowLayer()
    }

    private fun drawIslandHint(c: Canvas) {
        // System overlay owns the real Island; only a thin spacer hint if needed
        if (!cfg.island.enabled) return
        // leave top cutout area clear — OverlayService draws the live capsule
    }

    private fun drawApps(c: Canvas) {
        if (cfg.homeMode == HomeMode.DRAWER_ONLY) {
            p.color = 0xEEFFFFFF.toInt()
            p.textSize = 15f * d
            p.textAlign = Paint.Align.CENTER
            p.setShadowLayer(6f * d, 0f, 2f * d, 0xAA000000.toInt())
            c.drawText("Swipe up for App Drawer", width / 2f, height * 0.42f, p)
            p.clearShadowLayer()
            return
        }
        val cols = cfg.grid.columns.coerceIn(4, 9)
        val rows = cfg.grid.rows.coerceIn(4, 9)
        val left = 12f * d
        val top = 120f * d
        val bottom = height - 118f * d
        val cellW = (width - left * 2) / cols
        val cellH = (bottom - top) / rows
        val icon = min(cfg.grid.iconSize * d, min(cellW, cellH) * 0.52f)
        val start = page * pageSize()
        val slice = apps.drop(start).take(pageSize())

        slice.forEachIndexed { i, token ->
            if (token == dragging) return@forEachIndexed
            val col = i % cols
            val row = i / cols
            val cx = left + col * cellW + cellW / 2f
            val cy = top + row * cellH + cellH * 0.36f
            val s = if (token == pressed) 0.92f else 1f
            drawAppIcon(c, token, cx, cy, icon * s)
            if (cfg.grid.showLabels) {
                p.color = 0xF5FFFFFF.toInt()
                p.textSize = max(10f * d, cfg.grid.labelSize * d)
                p.typeface = Typeface.DEFAULT
                p.textAlign = Paint.Align.CENTER
                p.setShadowLayer(5f * d, 0f, 1.5f * d, 0xBB000000.toInt())
                c.drawText(label(token).take(11), cx, cy + icon / 2f + 15f * d, p)
                p.clearShadowLayer()
            }
        }

        // Page dots
        if (pageCount() > 1) {
            val dotsY = height - 100f * d
            val total = pageCount()
            val startX = width / 2f - (total - 1) * 7f * d
            for (i in 0 until total) {
                p.color = if (i == page) 0xFFFFFFFF.toInt() else 0x66FFFFFF.toInt()
                c.drawCircle(startX + i * 14f * d, dotsY, if (i == page) 3.5f * d else 2.5f * d, p)
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
        val r = RectF(16f * d, height - h - pad, width - 16f * d, height - pad)
        GlassPainter.drawGlass(c, r, cfg.dock.radius * d, 0xB0121822.toInt())
        val cell = r.width() / max(1, dockApps.size)
        val icon = min(48f * d, h * 0.55f)
        dockApps.forEachIndexed { i, pkg ->
            drawAppIcon(c, pkg, r.left + cell * i + cell / 2f, r.centerY(), icon)
        }
    }

    private fun drawHandle(c: Canvas) {
        p.color = 0x88FFFFFF.toInt()
        c.drawRoundRect(
            RectF(width / 2f - 18f * d, height - 10f * d, width / 2f + 18f * d, height - 6f * d),
            3f * d, 3f * d, p
        )
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
                if (dy < -80f * d && abs(dy) > abs(dx) * 1.2f) {
                    open(AetherSurface.DRAWER)
                    pressed = null
                    invalidate()
                    return true
                }
                if (dy > 80f * d && abs(dy) > abs(dx) * 1.2f && e.y < height * 0.35f) {
                    context.startActivity(Intent(context, AetherSearchActivity::class.java))
                    pressed = null
                    invalidate()
                    return true
                }
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
                    hit(e.x, e.y)?.let { launch(it) }
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
        val left = 12f * d
        val top = 120f * d
        val bottom = height - 118f * d
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
        val cell = (width - 32f * d) / max(1, dockApps.size)
        val idx = ((x - 16f * d) / cell).toInt()
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
