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
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.HomeMode
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Primary Aether surface: all installed apps are paged, persisted and draggable. */
class AetherHomeView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val settings = AetherSettingsStore(context)
    private val layout = AetherHomeLayoutStore(context)
    private val folders = AetherFolderStore(context)
    private val dock = AetherDockStore(context)
    private val history = AetherHistoryStore(context)
    private val handler = Handler(Looper.getMainLooper())
    private val density get() = resources.displayMetrics.density

    private var cfg = settings.load()
    private var order = mutableListOf<String>()
    private var page = 0
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var dragToken: String? = null
    private var dragOrigin = -1
    private var dragTarget = -1
    private var searchLock = false

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        order = layout.order(AetherRuntime.registry.launcher.apps().map { it.packageName })
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        cfg = settings.load()
        syncOrder()
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = LinearGradient(0f, 0f, 0f, h, 0x12030810, 0x08030810, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        drawHeader(canvas, w)
        drawIsland(canvas, w)
        drawGrid(canvas, w, h)
        drawDock(canvas, w, h)
        drawPageDots(canvas, w, h)
        drawEdge(canvas, w, h)
        drawDragged(canvas)
    }

    private fun pageSize(): Int = cfg.grid.columns.coerceIn(4, 8) * cfg.grid.rows.coerceIn(4, 10)
    private fun pageCount(): Int = max(1, (order.size + pageSize() - 1) / pageSize())

    private fun syncOrder() {
        val installed = AetherRuntime.registry.launcher.apps().map { it.packageName }
        if (installed.any { it !in order } || order.none { it in installed }) {
            order = layout.order(installed)
        }
        page = page.coerceIn(0, pageCount() - 1)
    }

    private fun drawHeader(c: Canvas, w: Float) {
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 11f * density
        paint.color = 0xAFFFFFFF.toInt()
        c.drawText(SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault()), 24f * density, 40f * density, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 27f * density
        c.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f * density, 70f * density, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f * density
        paint.color = 0x9AFFFFFF.toInt()
        c.drawText("AETHER", w - 24f * density, 43f * density, paint)
        paint.textSize = 7f * density
        paint.color = 0x62FFFFFF
        c.drawText("LIQUID / FLUID / PRIVATE", w - 24f * density, 57f * density, paint)
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawIsland(c: Canvas, w: Float) {
        if (!cfg.island.enabled) return
        val activity = AetherRuntime.registry.island.activity
        val islandWidth = min(w * .56f, cfg.island.width * density * if (activity.title.isBlank()) 1f else 1.5f)
        val height = max(30f * density, cfg.island.height * density)
        val y = max(7f * density, cfg.island.topOffset * density)
        val rect = RectF(w / 2f - islandWidth / 2f, y, w / 2f + islandWidth / 2f, y + height)
        drawGlass(c, rect, height / 2f, 0xB914171D.toInt())
        if (cfg.island.cutoutAware) {
            val cutout = min(34f * density, islandWidth * .30f)
            paint.color = 0xEE000000.toInt()
            c.drawRoundRect(RectF(w / 2f - cutout / 2f, rect.top - 1f, w / 2f + cutout / 2f, rect.bottom + 1f), height / 2f, height / 2f, paint)
        }
        if (activity.title.isNotBlank()) {
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 10f * density
            c.drawText(activity.title.take(25), w / 2f, rect.centerY() + 4f * density, paint)
            paint.typeface = Typeface.DEFAULT
        }
    }

    private fun drawGrid(c: Canvas, w: Float, h: Float) {
        if (cfg.homeMode == HomeMode.DRAWER_ONLY) return
        val columns = cfg.grid.columns.coerceIn(4, 8)
        val left = cfg.grid.horizontalPadding * density
        val gap = cfg.grid.horizontalSpacing * density
        val cell = (w - left * 2f - gap * (columns - 1)) / columns
        val top = 112f * density
        val reserve = if (cfg.dock.enabled) cfg.dock.height * density + cfg.dock.bottomPadding * density + 34f * density else 14f * density
        val available = max(200f * density, h - top - reserve)
        val rowGap = cfg.grid.verticalSpacing * density
        val rows = cfg.grid.rows.coerceIn(4, 10)
        val rowHeight = max(72f * density, (available - rowGap * (rows - 1)) / rows)
        val iconSize = min(cfg.grid.iconSize * density, cell * .66f).coerceIn(34f * density, 70f * density)
        val start = page * pageSize()

        order.drop(start).take(pageSize()).forEachIndexed { localIndex, token ->
            val absoluteIndex = start + localIndex
            if (token == dragToken) return@forEachIndexed
            val col = localIndex % columns
            val row = localIndex / columns
            val centerX = left + col * (cell + gap) + cell / 2f
            val y = top + row * (rowHeight + rowGap)
            val iconRect = RectF(centerX - iconSize / 2f, y, centerX + iconSize / 2f, y + iconSize)
            if (token.startsWith("folder:")) folderIcon(c, iconRect, token.removePrefix("folder:"))
            else drawIcon(c, AetherRuntime.registry.launcher.icon(token), iconRect)
            if (cfg.grid.showLabels) {
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10.5f * density
                paint.color = 0xE8FFFFFF.toInt()
                c.drawText(label(token), centerX, iconRect.bottom + 17f * density, paint)
            }
            if (absoluteIndex == dragTarget && dragToken != null) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f * density
                paint.color = 0xBFFFFFFF.toInt()
                c.drawRoundRect(RectF(centerX - iconSize * .60f, y - iconSize * .08f, centerX + iconSize * .60f, y + iconSize * 1.14f), 18f * density, 18f * density, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun drawDock(c: Canvas, w: Float, h: Float) {
        if (!cfg.dock.enabled) return
        val bottom = h - cfg.dock.bottomPadding * density
        val top = bottom - cfg.dock.height * density
        val rect = RectF(14f * density, top, w - 14f * density, bottom)
        drawGlass(c, rect, cfg.dock.radius * density, 0xA91A1E26.toInt())
        val packages = (dock.favorites() + order.filterNot { it.startsWith("folder:") }).distinct().take(cfg.dock.appCount.coerceIn(3, 8))
        if (packages.isEmpty()) return
        val slot = rect.width() / packages.size
        val iconSize = min(56f * density, rect.height() * .68f)
        packages.forEachIndexed { index, pkg ->
            val centerX = rect.left + slot * (index + .5f)
            drawIcon(c, AetherRuntime.registry.launcher.icon(pkg), RectF(centerX - iconSize / 2f, rect.centerY() - iconSize / 2f, centerX + iconSize / 2f, rect.centerY() + iconSize / 2f))
        }
    }

    private fun drawPageDots(c: Canvas, w: Float, h: Float) {
        if (pageCount() <= 1) return
        val y = h - if (cfg.dock.enabled) cfg.dock.height * density + cfg.dock.bottomPadding * density + 7f * density else 12f * density
        val gap = 8f * density
        val total = (pageCount() - 1) * gap
        for (i in 0 until pageCount()) {
            paint.color = if (i == page) 0xF2FFFFFF.toInt() else 0x55FFFFFF
            c.drawCircle(w / 2f + i * gap - total / 2f, y, if (i == page) 3f * density else 2f * density, paint)
        }
    }

    private fun drawEdge(c: Canvas, w: Float, h: Float) {
        if (!cfg.quickSpace.enabled) return
        val x = if (cfg.quickSpace.edge == QuickEdge.RIGHT) w - 5f * density else 5f * density
        paint.color = 0xDFFFFFFF.toInt()
        c.drawRoundRect(RectF(x - 2f * density, h * .46f, x + 2f * density, h * .55f), 3f * density, 3f * density, paint)
    }

    private fun drawGlass(c: Canvas, rect: RectF, radius: Float, base: Int) {
        paint.color = base
        paint.setShadowLayer(22f * density, 0f, 9f * density, 0x70000000)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()
        paint.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0x4AFFFFFF, 0x0CFFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(rect.left + 1f, rect.top + 1f, rect.right - 1f, rect.bottom - 1f), radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, density)
        paint.color = 0x5CFFFFFF.toInt()
        c.drawRoundRect(RectF(rect.left + 1f, rect.top + 1f, rect.right - 1f, rect.bottom - 1f), radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, rect.top, 0f, rect.top + rect.height() * .25f, 0x35FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
    }

    private fun drawIcon(c: Canvas, drawable: Drawable?, rect: RectF) {
        drawable ?: return
        drawable.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        drawable.draw(c)
    }

    private fun folderIcon(c: Canvas, rect: RectF, id: String) {
        drawGlass(c, rect, rect.width() * .25f, 0xB91D222A.toInt())
        val packages = folders.load().firstOrNull { it.id == id }?.packages.orEmpty().take(4)
        val mini = rect.width() * .30f
        packages.forEachIndexed { index, pkg ->
            val x = rect.left + rect.width() * .18f + (index % 2) * (mini + rect.width() * .12f)
            val y = rect.top + rect.height() * .18f + (index / 2) * (mini + rect.height() * .12f)
            drawIcon(c, AetherRuntime.registry.launcher.icon(pkg), RectF(x, y, x + mini, y + mini))
        }
    }

    private fun label(token: String): String = if (token.startsWith("folder:")) {
        folders.load().firstOrNull { it.id == token.removePrefix("folder:") }?.name ?: "Folder"
    } else {
        AetherRuntime.registry.launcher.apps().firstOrNull { it.packageName == token }?.label ?: "App"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; dragX = event.x; dragY = event.y
                dragOrigin = hit(event.x, event.y); dragTarget = dragOrigin
                if (dragOrigin >= 0) {
                    handler.postDelayed({
                        if (abs(dragX - downX) < 16f * density && abs(dragY - downY) < 16f * density) {
                            dragToken = order.getOrNull(dragOrigin)
                            invalidate()
                        }
                    }, 430L)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                dragX = event.x; dragY = event.y
                if (dragToken != null) { dragTarget = hit(event.x, event.y); invalidate() }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                val dx = event.x - downX
                val dy = event.y - downY
                if (dragToken != null) { finishDrag(); return true }
                if (cfg.quickSpace.enabled && abs(dx) > cfg.quickSpace.triggerDistance * density &&
                    ((cfg.quickSpace.edge == QuickEdge.RIGHT && downX > width - 110f * density) || (cfg.quickSpace.edge == QuickEdge.LEFT && downX < 110f * density))) {
                    open(AetherSurface.QUICK); return true
                }
                if (dy > 95f * density && !searchLock) {
                    if (cfg.homeMode == HomeMode.ALL_APPS) {
                        searchLock = true
                        context.startActivity(Intent(context, AetherSearchActivity::class.java))
                        postDelayed({ searchLock = false }, 450L)
                    } else if (page > 0) { page--; invalidate() }
                    return true
                }
                if (dy < -95f * density) {
                    if (page < pageCount() - 1) page++
                    else if (cfg.homeMode != HomeMode.ALL_APPS) open(AetherSurface.DRAWER)
                    invalidate(); return true
                }
                if (abs(dx) > 90f * density && abs(dy) < 60f * density && cfg.homeMode == HomeMode.ALL_APPS) {
                    page = if (dx < 0) min(page + 1, pageCount() - 1) else max(page - 1, 0)
                    invalidate(); return true
                }
                if (abs(dx) < 22f * density && abs(dy) < 22f * density && dragOrigin >= 0) {
                    launch(order.getOrNull(dragOrigin)); return true
                }
            }
        }
        return true
    }

    private fun finishDrag() {
        val token = dragToken ?: return
        val target = dragTarget
        if (target >= 0 && target < order.size && target != dragOrigin) {
            val other = order[target]
            if (!token.startsWith("folder:") && !other.startsWith("folder:") && other != token) merge(token, other)
            else {
                order.remove(token)
                order.add(target.coerceIn(0, order.size), token)
                layout.save(order)
            }
        }
        dragToken = null; dragOrigin = -1; dragTarget = -1; invalidate()
    }

    private fun merge(a: String, b: String) {
        val existing = folders.load().firstOrNull { a in it.packages || b in it.packages }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val packages = (existing?.packages.orEmpty() + a + b).distinct()
        val name = existing?.name ?: "${label(a)} & ${label(b)}"
        folders.save(AetherFolder(id, name, packages))
        val position = minOf(order.indexOf(a), order.indexOf(b)).coerceAtLeast(0)
        order.removeAll { it == a || it == b }
        order.add(position.coerceAtMost(order.size), "folder:$id")
        layout.save(order)
        page = (position / pageSize()).coerceIn(0, pageCount() - 1)
    }

    private fun hit(x: Float, y: Float): Int {
        if (cfg.homeMode == HomeMode.DRAWER_ONLY || y < 104f * density) return -1
        val columns = cfg.grid.columns.coerceIn(4, 8)
        val left = cfg.grid.horizontalPadding * density
        val gap = cfg.grid.horizontalSpacing * density
        val cell = (width - left * 2f - gap * (columns - 1)) / columns
        val top = 112f * density
        val reserve = if (cfg.dock.enabled) cfg.dock.height * density + cfg.dock.bottomPadding * density + 34f * density else 14f * density
        val available = max(200f * density, height - top - reserve)
        val rows = cfg.grid.rows.coerceIn(4, 10)
        val rowGap = cfg.grid.verticalSpacing * density
        val rowHeight = max(72f * density, (available - rowGap * (rows - 1)) / rows)
        val col = ((x - left) / (cell + gap)).toInt()
        val row = ((y - top) / (rowHeight + rowGap)).toInt()
        if (col !in 0 until columns || row !in 0 until rows) return -1
        val absolute = page * pageSize() + row * columns + col
        return if (absolute in order.indices) absolute else -1
    }

    private fun launch(token: String?) {
        if (token.isNullOrBlank()) return
        if (token.startsWith("folder:")) { open(AetherSurface.FOLDER, token.removePrefix("folder:")); return }
        history.record(token)
        AetherRuntime.registry.launcher.launchIntent(token)?.let { context.startActivity(it) }
    }

    private fun open(surface: String, folderId: String? = null) {
        context.startActivity(Intent(context, AetherSurfaceActivity::class.java).apply {
            putExtra("surface", surface)
            folderId?.let { putExtra("folder_id", it) }
        })
    }

    private fun drawDragged(c: Canvas) {
        val token = dragToken ?: return
        val size = 70f * density
        val rect = RectF(dragX - size / 2f, dragY - size / 2f, dragX + size / 2f, dragY + size / 2f)
        drawGlass(c, rect, 23f * density, 0xD1222730.toInt())
        if (token.startsWith("folder:")) folderIcon(c, RectF(rect.left + 6f, rect.top + 6f, rect.right - 6f, rect.bottom - 6f), token.removePrefix("folder:"))
        else drawIcon(c, AetherRuntime.registry.launcher.icon(token), RectF(rect.left + 7f, rect.top + 7f, rect.right - 7f, rect.bottom - 7f))
    }
}
