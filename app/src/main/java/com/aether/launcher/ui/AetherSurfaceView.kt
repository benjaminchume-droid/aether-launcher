package com.aether.launcher.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.aether.launcher.AetherFolderStore
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.settings.AetherSettingsActivity
import com.aether.launcher.system.AetherSystemActions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object AetherSurface {
    const val DRAWER = "drawer"
    const val HISTORY = "history"
    const val QUICK = "quick"
    const val CONTROL = "control"
    const val NOTIFICATIONS = "notifications"
    const val WIDGETS = "widgets"
    const val FOLDER = "folder"
    const val MULTITASK = "multitask"
    const val SETUP_CENTER = "setup_center"
    const val ABOUT = "about"
}

/** Draws glass UI with transparent background so wallpaper shows through. */
class AetherSurfaceView(
    c: Context,
    private val surface: String,
    private val folderId: String?
) : View(c) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d get() = resources.displayMetrics.density
    private val apps get() = AetherRuntime.registry.launcher.apps()
    private val history = AetherHistoryStore(c)
    private val folders = AetherFolderStore(c)
    private var downX = 0f
    private var downY = 0f
    private var brightnessTouch = false
    private var volumeTouch = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        // Light dim only — wallpaper from FLAG_SHOW_WALLPAPER / GlassRoot stays visible
        p.color = 0x33000000
        c.drawRect(0f, 0f, w, height.toFloat(), p)

        when (surface) {
            AetherSurface.QUICK -> quick(c, w)
            AetherSurface.DRAWER -> drawer(c, w)
            AetherSurface.FOLDER -> folder(c, w)
            AetherSurface.HISTORY -> history(c, w)
            AetherSurface.CONTROL -> control(c, w, height.toFloat())
            AetherSurface.NOTIFICATIONS -> notifications(c, w)
            AetherSurface.MULTITASK -> multi(c, w)
            AetherSurface.WIDGETS -> widgets(c, w)
            else -> drawer(c, w)
        }
    }

    private fun header(c: Canvas, title: String, sub: String) {
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 28f * d
        p.color = Color.WHITE
        p.setShadowLayer(10f * d, 0f, 3f * d, 0xAA000000.toInt())
        c.drawText(title, 24f * d, 72f * d, p)
        p.typeface = Typeface.DEFAULT
        p.textSize = 12f * d
        p.color = 0xE0FFFFFF.toInt()
        c.drawText(sub, 24f * d, 94f * d, p)
        p.clearShadowLayer()
        p.textAlign = Paint.Align.CENTER
    }

    private fun glass(c: Canvas, r: RectF, rad: Float, base: Int = 0xB0161B23.toInt()) {
        GlassPainter.drawGlass(c, r, rad, base)
    }

    private fun tile(c: Canvas, r: RectF, title: String, detail: String = "", accent: Boolean = false) {
        glass(c, r, min(22f * d, r.height() * 0.3f), if (accent) 0xC04C83D8.toInt() else 0xB0151A22.toInt())
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 15f * d
        p.color = Color.WHITE
        c.drawText(title, r.left + 16f * d, r.top + 28f * d, p)
        if (detail.isNotBlank()) {
            p.typeface = Typeface.DEFAULT
            p.textSize = 11f * d
            p.color = 0xCCFFFFFF.toInt()
            c.drawText(detail.take(28), r.left + 16f * d, r.top + 48f * d, p)
        }
        p.textAlign = Paint.Align.CENTER
    }

    private fun icon(c: Canvas, pkg: String, r: RectF) {
        AetherRuntime.registry.launcher.icon(pkg)?.let { dr: Drawable ->
            dr.setBounds(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
            dr.draw(c)
        }
    }

    private fun quick(c: Canvas, w: Float) {
        header(c, "Quick Space", "Notes, tools, recents — over your wallpaper")
        val items = listOf(
            "Notes" to "Write something",
            "Voice" to "Record audio",
            "Calculator" to "Quick math",
            "Clipboard" to "Paste board",
            "Recent Apps" to "Jump back",
            "Control Center" to "Toggles & media",
            "Widgets" to "Home widgets",
            "Settings" to "Aether settings"
        )
        val gap = 12f * d
        val left = 16f * d
        val cell = (w - left * 2 - gap) / 2f
        items.forEachIndexed { i, (name, detail) ->
            val col = i % 2
            val row = i / 2
            val x = left + col * (cell + gap)
            val y = 118f * d + row * 76f * d
            tile(c, RectF(x, y, x + cell, y + 64f * d), name, detail, i == 0)
        }
    }

    private fun drawer(c: Canvas, w: Float) {
        header(c, "All Apps", "${apps.size} installed • swipe down to close")
        val list = apps
        val cols = 4
        val gap = 10f * d
        val left = 16f * d
        val top = 120f * d
        val cell = (w - left * 2 - gap * (cols - 1)) / cols
        val size = min(52f * d, cell * 0.58f)
        list.take(80).forEachIndexed { i, a ->
            val col = i % cols
            val row = i / cols
            val x = left + col * (cell + gap)
            val y = top + row * 88f * d
            glass(c, RectF(x, y, x + cell, y + 76f * d), 20f * d, 0x99141920.toInt())
            icon(c, a.packageName, RectF(x + cell / 2 - size / 2, y + 8f * d, x + cell / 2 + size / 2, y + 8f * d + size))
            p.textAlign = Paint.Align.CENTER
            p.textSize = 10f * d
            p.color = 0xF0FFFFFF.toInt()
            c.drawText(a.label.take(10), x + cell / 2, y + 70f * d, p)
        }
    }

    private fun control(c: Canvas, w: Float, h: Float) {
        AetherRuntime.registry.controlCenter.refresh()
        val st = AetherRuntime.registry.controlCenter.controls
        header(c, "Control Center", "Live system controls")

        val mediaR = RectF(16f * d, 110f * d, w * 0.52f - 6f * d, 220f * d)
        glass(c, mediaR, 24f * d, 0xB0181C26.toInt())
        p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 14f * d
        p.color = Color.WHITE
        c.drawText(st.mediaTitle.take(18).ifBlank { "Not Playing" }, mediaR.left + 16f * d, mediaR.top + 36f * d, p)
        p.typeface = Typeface.DEFAULT
        p.textSize = 12f * d
        p.color = 0xAAFFFFFF.toInt()
        c.drawText(st.mediaArtist.take(20), mediaR.left + 16f * d, mediaR.top + 56f * d, p)
        p.textAlign = Paint.Align.CENTER
        p.color = Color.WHITE
        p.textSize = 22f * d
        c.drawText("\u23EE  \u25B6  \u23ED", mediaR.centerX(), mediaR.bottom - 28f * d, p)

        glass(c, RectF(w * 0.52f + 6f * d, 110f * d, w - 16f * d, 160f * d), 20f * d)
        p.textAlign = Paint.Align.LEFT
        p.textSize = 14f * d
        p.color = Color.WHITE
        c.drawText("Wi-Fi", w * 0.52f + 20f * d, 142f * d, p)

        glass(c, RectF(w * 0.52f + 6f * d, 170f * d, w - 16f * d, 220f * d), 20f * d)
        c.drawText("Bluetooth", w * 0.52f + 20f * d, 202f * d, p)

        val toggleY = 240f * d
        val toggleSize = 56f * d
        listOf(st.torch to "Torch", st.rotation to "Rotate", st.dnd to "Focus", st.airplane to "Air")
            .forEachIndexed { i, (on, _) ->
                val x = 16f * d + i * (toggleSize + 14f * d)
                glass(c, RectF(x, toggleY, x + toggleSize, toggleY + toggleSize), toggleSize / 2f,
                    if (on) 0xC03D7EFF.toInt() else 0xA0181C26.toInt())
            }

        val brightR = RectF(16f * d, 316f * d, 70f * d, 460f * d)
        glass(c, brightR, 26f * d)
        p.color = 0x66FFFFFF.toInt()
        val fillH = brightR.height() * st.brightness
        c.drawRoundRect(RectF(brightR.left + 4f * d, brightR.bottom - fillH, brightR.right - 4f * d, brightR.bottom - 4f * d), 18f * d, 18f * d, p)

        val volR = RectF(84f * d, 316f * d, 138f * d, 460f * d)
        glass(c, volR, 26f * d)
        val volH = volR.height() * st.volume
        c.drawRoundRect(RectF(volR.left + 4f * d, volR.bottom - volH, volR.right - 4f * d, volR.bottom - 4f * d), 18f * d, 18f * d, p)
    }

    private fun history(c: Canvas, w: Float) {
        header(c, "Recent", "Your launch history")
        val list = history.load().mapNotNull { e -> apps.firstOrNull { it.packageName == e.packageName } }
            .distinctBy { it.packageName }.take(12)
        if (list.isEmpty()) {
            tile(c, RectF(16f * d, 120f * d, w - 16f * d, 190f * d), "Nothing yet", "Launch apps and they appear here")
        } else {
            list.forEachIndexed { i, a ->
                val y = 118f * d + i * 68f * d
                glass(c, RectF(16f * d, y, w - 16f * d, y + 58f * d), 18f * d)
                icon(c, a.packageName, RectF(28f * d, y + 8f * d, 70f * d, y + 50f * d))
                p.textAlign = Paint.Align.LEFT
                p.textSize = 15f * d
                p.color = Color.WHITE
                c.drawText(a.label, 84f * d, y + 36f * d, p)
            }
        }
    }

    private fun folder(c: Canvas, w: Float) {
        val f = folders.load().firstOrNull { it.id == folderId }
        header(c, f?.name ?: "Folder", "Apps in this folder")
        f?.packages.orEmpty().mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .forEachIndexed { i, a ->
                val cell = (w - 48f * d) / 3
                val x = 16f * d + (i % 3) * (cell + 8f * d)
                val y = 120f * d + (i / 3) * 100f * d
                glass(c, RectF(x, y, x + cell, y + 88f * d), 20f * d)
                icon(c, a.packageName, RectF(x + cell / 2 - 24f * d, y + 10f * d, x + cell / 2 + 24f * d, y + 58f * d))
                p.textAlign = Paint.Align.CENTER
                p.textSize = 11f * d
                p.color = Color.WHITE
                c.drawText(a.label.take(12), x + cell / 2, y + 78f * d, p)
            }
    }

    private fun notifications(c: Canvas, w: Float) {
        header(c, "Activity", "Live notifications")
        val ns = AetherRuntime.registry.notifications.all()
        if (ns.isEmpty()) {
            tile(c, RectF(16f * d, 120f * d, w - 16f * d, 200f * d), "No live activity", "Grant Notification Access in Settings")
        } else {
            ns.take(8).forEachIndexed { i, n ->
                val y = 118f * d + i * 76f * d
                tile(c, RectF(16f * d, y, w - 16f * d, y + 66f * d), n.title.ifBlank { "Notification" }, n.text.take(40))
            }
        }
    }

    private fun multi(c: Canvas, w: Float) {
        header(c, "Multitask", "Recent apps")
        history.load().mapNotNull { e -> apps.firstOrNull { it.packageName == e.packageName } }
            .distinctBy { it.packageName }.take(6).forEachIndexed { i, a ->
                val y = 118f * d + i * 78f * d
                tile(c, RectF(16f * d, y, w - 16f * d, y + 68f * d), a.label, "Tap to reopen")
            }
    }

    private fun widgets(c: Canvas, w: Float) {
        header(c, "Widgets", "Aether widget host")
        tile(c, RectF(16f * d, 120f * d, w - 16f * d, 210f * d), "Live Widgets", "Open widget host")
        tile(c, RectF(16f * d, 226f * d, w - 16f * d, 296f * d), "+ Add widget", "Pick a provider")
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y
                if (surface == AetherSurface.CONTROL) {
                    if (e.x in 16f * d..70f * d && e.y in 316f * d..460f * d) brightnessTouch = true
                    if (e.x in 84f * d..138f * d && e.y in 316f * d..460f * d) volumeTouch = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (brightnessTouch) {
                    AetherRuntime.registry.controlCenter.setBrightness(((460f * d - e.y) / (144f * d)).coerceIn(0f, 1f))
                    invalidate()
                }
                if (volumeTouch) {
                    AetherRuntime.registry.controlCenter.setVolume(((460f * d - e.y) / (144f * d)).coerceIn(0f, 1f))
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                brightnessTouch = false
                volumeTouch = false
                val dy = e.y - downY
                if (dy > 100f * d) {
                    (context as? Activity)?.finish()
                    return true
                }
                if (surface == AetherSurface.CONTROL && abs(e.x - downX) < 24f * d && abs(dy) < 24f * d) {
                    handleControlTap(e.x, e.y)
                    return true
                }
                if (surface == AetherSurface.QUICK && abs(dy) < 24f * d && e.y > 110f * d) {
                    val gap = 12f * d
                    val left = 16f * d
                    val cell = (width - left * 2 - gap) / 2f
                    val col = ((e.x - left) / (cell + gap)).toInt()
                    val row = ((e.y - 118f * d) / (76f * d)).toInt()
                    when (row * 2 + col) {
                        0 -> note()
                        1 -> Toast.makeText(context, "Allow microphone for voice notes", Toast.LENGTH_SHORT).show()
                        2 -> calculator()
                        3 -> {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val t = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                            Toast.makeText(context, t?.take(80) ?: "Clipboard empty", Toast.LENGTH_LONG).show()
                        }
                        4 -> context.startActivity(Intent(context, AetherSurfaceActivity::class.java).putExtra("surface", AetherSurface.HISTORY))
                        5 -> context.startActivity(Intent(context, AetherSurfaceActivity::class.java).putExtra("surface", AetherSurface.CONTROL))
                        6 -> context.startActivity(Intent(context, AetherWidgetHostActivity::class.java))
                        7 -> context.startActivity(Intent(context, AetherSettingsActivity::class.java))
                    }
                    return true
                }
                if (surface == AetherSurface.DRAWER && abs(dy) < 24f * d) {
                    val cols = 4
                    val gap = 10f * d
                    val left = 16f * d
                    val top = 120f * d
                    val cell = (width - left * 2 - gap * (cols - 1)) / cols
                    val col = ((e.x - left) / (cell + gap)).toInt()
                    val row = ((e.y - top) / (88f * d)).toInt()
                    val idx = row * cols + col
                    apps.getOrNull(idx)?.let { AetherRuntime.registry.launcher.launch(it.packageName) }
                }
            }
        }
        return true
    }

    private fun handleControlTap(x: Float, y: Float) {
        val toggleY = 240f * d
        val toggleSize = 56f * d
        if (y in toggleY..(toggleY + toggleSize)) {
            val idx = ((x - 16f * d) / (toggleSize + 14f * d)).toInt()
            when (idx) {
                0 -> { AetherRuntime.registry.controlCenter.toggleTorch(); invalidate() }
                1 -> { AetherRuntime.registry.controlCenter.toggleRotation(); invalidate() }
                2 -> AetherSystemActions.toggleDnd(context)
                3 -> AetherSystemActions.openAirplane(context)
            }
        }
        if (y in 110f * d..160f * d && x > width * 0.52f) AetherSystemActions.openWifi(context)
        if (y in 170f * d..220f * d && x > width * 0.52f) AetherSystemActions.openBluetooth(context)
    }

    private fun note() {
        val input = EditText(context).apply { hint = "Write a note\u2026" }
        AlertDialog.Builder(context)
            .setTitle("New note")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                AetherRuntime.registry.notes.capture(input.text.toString())
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun calculator() {
        val input = EditText(context).apply { hint = "12 + 8" }
        AlertDialog.Builder(context)
            .setTitle("Calculator")
            .setView(input)
            .setPositiveButton("=") { _, _ ->
                val s = input.text.toString().replace(" ", "")
                val result = runCatching {
                    val parts = s.split("+")
                    if (parts.size == 2) (parts[0].toDouble() + parts[1].toDouble()).toString() else s
                }.getOrElse { "?" }
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
