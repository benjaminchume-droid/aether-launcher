package com.aether.launcher.system

import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.settings.AetherSettingsStore
import kotlin.math.max
import kotlin.math.min

/**
 * Live notification / activity capsule.
 * Matches reference styles: compact pill, expanded media/call/download,
 * and Quick Reply composer that stays until the real notification changes.
 */
class AetherNotificationIslandOverlay(private val service: AetherOverlayService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val store = AetherSettingsStore(service)
    private var view: CapsuleView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var expanded = false
    private var key = ""
    private val d get() = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    fun show() {
        if (view != null || !Settings.canDrawOverlays(service)) return
        val c = store.load().island
        val v = CapsuleView(service)
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val p = WindowManager.LayoutParams(
            dp(c.width.coerceAtLeast(92)),
            dp(c.height.coerceAtLeast(28)),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        place(p)
        runCatching {
            wm.addView(v, p)
            view = v
            lp = p
            v.setOnClickListener { toggle() }
            v.setOnLongClickListener {
                service.startActivity(
                    android.content.Intent(service, NotificationSettingsActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }
            refresh()
        }
    }

    private fun place(p: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = wm.currentWindowMetrics.windowInsets
            val top = i.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            val cut = i.displayCutout?.boundingRects?.maxByOrNull { it.width() * it.height() }
            p.y = if (cut != null && store.load().island.cutoutAware)
                max(top, cut.bottom) + dp(3)
            else top + dp(store.load().island.topOffset)
            if (cut != null && store.load().island.cutoutAware) {
                p.width = max(p.width, cut.width() + dp(54))
            }
        } else {
            p.y = dp(store.load().island.topOffset + 24)
        }
    }

    fun refresh() {
        val v = view ?: return
        val a = AetherRuntime.registry.island.activity
        if (a.key != key && key.isNotBlank()) expanded = false
        key = a.key
        v.activity = a
        v.replyable = a.type == ActivityType.NOTIFICATION && NotificationReplyBridge.canReply(a.key)

        val active = a.type != ActivityType.NONE && a.title.isNotBlank()
        if (!active) expanded = false

        val c = store.load().island
        val w = when {
            expanded && a.type == ActivityType.MEDIA -> dp(min(340, 300))
            expanded && a.type == ActivityType.CALL -> dp(min(320, 280))
            expanded && a.type == ActivityType.DOWNLOAD -> dp(min(320, 280))
            expanded && a.type == ActivityType.NOTIFICATION -> dp(min(360, 300))
            expanded && active -> dp(min(320, max(c.width + 100, 260)))
            else -> dp(c.width.coerceAtLeast(92))
        }
        val h = when {
            expanded && a.type == ActivityType.NOTIFICATION && v.replyable -> dp(168)
            expanded && a.type == ActivityType.MEDIA -> dp(96)
            expanded && a.type == ActivityType.CALL -> dp(72)
            expanded && a.type == ActivityType.DOWNLOAD -> dp(68)
            expanded && active -> dp(max(c.height + 50, 78))
            else -> dp(c.height.coerceAtLeast(28))
        }
        resize(w, h)
        v.invalidate()
    }

    private fun resize(w: Int, h: Int) {
        val p = lp ?: return
        val v = view ?: return
        p.width = w
        p.height = h
        place(p)
        runCatching { wm.updateViewLayout(v, p) }
    }

    private fun toggle() {
        val a = AetherRuntime.registry.island.activity
        if (a.type == ActivityType.NONE) return
        expanded = !expanded
        lp?.let {
            it.flags = if (expanded)
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            else
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            runCatching { wm.updateViewLayout(view, it) }
        }
        refresh()
        if (expanded && a.type == ActivityType.NOTIFICATION && NotificationReplyBridge.canReply(a.key)) {
            view?.postDelayed({ view?.focusReply() }, 140)
        }
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        lp = null
    }
}

private class CapsuleView(c: Context) : FrameLayout(c) {

    var activity = IslandActivity(ActivityType.NONE, "")
    var replyable = false

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var input: EditText? = null
    private var send: TextView? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    init {
        setWillNotDraw(false)
    }

    fun focusReply() {
        input?.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (replyable && h > 120f * resources.displayMetrics.density) composer()
        else clearComposer()
    }

    private fun composer() {
        if (input != null) return

        // Matches Quick Reply reference: dark bubble + bright blue send
        input = EditText(context).apply {
            hint = "Reply…"
            setTextColor(Color.WHITE)
            setHintTextColor(0x99FFFFFF.toInt())
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            background = bg(0xE6282C34.toInt(), 26)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, id, _ ->
                if (id == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendReply(); true
                } else false
            }
        }

        send = TextView(context).apply {
            text = "→"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = bg(0xFF1E88FF.toInt(), 26)
            setOnClickListener { sendReply() }
        }

        addView(input, LayoutParams(-1, dp(48)).apply {
            leftMargin = dp(14)
            rightMargin = dp(66)
            topMargin = dp(108)
        })
        addView(send, LayoutParams(dp(48), dp(48)).apply {
            gravity = Gravity.END
            rightMargin = dp(14)
            topMargin = dp(108)
        })
    }

    private fun clearComposer() {
        input?.let { removeView(it) }
        send?.let { removeView(it) }
        input = null
        send = null
    }

    private fun bg(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun sendReply() {
        val text = input?.text?.toString().orEmpty()
        if (NotificationReplyBridge.reply(activity.key, text)) {
            input?.setText("")
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(h / 2f, 28f * d)

        // Dark glass body
        p.color = 0xF20C0E12.toInt()
        canvas.drawRoundRect(RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f), r, r, p)

        // Subtle top highlight
        p.shader = LinearGradient(
            0f, 0f, 0f, h * 0.45f,
            0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), r, r, p)
        p.shader = null

        // Border
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, d * 0.9f)
        p.color = 0x55FFFFFF.toInt()
        canvas.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), r, r, p)
        p.style = Paint.Style.FILL

        val expanded = h > 55f * d
        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()

        // Leading glyph / progress indicator
        when (activity.type) {
            ActivityType.RECORDING -> {
                p.color = 0xFFFF5252.toInt()
                canvas.drawCircle(22f * d, min(h * 0.5f, 20f * d), 4.5f * d, p)
            }
            ActivityType.CALL -> {
                p.color = 0xFF69F0AE.toInt()
                canvas.drawCircle(22f * d, min(h * 0.5f, 20f * d), 4.5f * d, p)
            }
            ActivityType.DOWNLOAD -> {
                p.color = 0xFF69F0AE.toInt()
                canvas.drawCircle(22f * d, min(h * 0.5f, 20f * d), 4.5f * d, p)
            }
            ActivityType.MEDIA -> {
                p.color = 0xFFEA80FC.toInt()
                canvas.drawCircle(22f * d, min(h * 0.5f, 20f * d), 4.5f * d, p)
            }
            else -> {
                p.color = 0xAAFFFFFF.toInt()
                canvas.drawCircle(22f * d, min(h * 0.5f, 20f * d), 3.5f * d, p)
            }
        }

        // Title
        p.color = 0xF5FFFFFF.toInt()
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = (if (expanded) 13f else 11f) * d
        p.textAlign = Paint.Align.LEFT
        val title = if (active) activity.title.take(if (expanded) 34 else 22) else "AETHER"
        canvas.drawText(title, 36f * d, min(h * 0.52f, 22f * d), p)

        if (!expanded) return

        // Detail / progress
        p.typeface = Typeface.DEFAULT
        p.textSize = 11f * d
        p.color = 0xB8FFFFFF.toInt()
        if (activity.detail.isNotBlank()) {
            canvas.drawText(activity.detail.take(42), 36f * d, 40f * d, p)
        }

        when (activity.type) {
            ActivityType.MEDIA -> drawMediaControls(canvas, d, w, h)
            ActivityType.CALL -> drawCallActions(canvas, d, w, h)
            ActivityType.DOWNLOAD -> drawProgress(canvas, d, w, h)
            ActivityType.NOTIFICATION -> {
                if (!replyable) {
                    p.textSize = 11f * d
                    p.color = 0x99FFFFFF.toInt()
                    canvas.drawText("Tap to open", 36f * d, 62f * d, p)
                }
            }
            else -> {}
        }
    }

    private fun drawMediaControls(c: Canvas, d: Float, w: Float, h: Float) {
        // Progress bar
        if (activity.progress in 0f..1f) {
            p.color = 0x33FFFFFF
            c.drawRoundRect(RectF(36f * d, 52f * d, w - 36f * d, 56f * d), 2f * d, 2f * d, p)
            p.color = 0xFFEA80FC.toInt()
            val filled = 36f * d + (w - 72f * d) * activity.progress
            c.drawRoundRect(RectF(36f * d, 52f * d, filled, 56f * d), 2f * d, 2f * d, p)
        }
        p.color = Color.WHITE
        p.textSize = 20f * d
        p.textAlign = Paint.Align.CENTER
        c.drawText("⏮   ⏸   ⏭", w / 2f, 82f * d, p)
    }

    private fun drawCallActions(c: Canvas, d: Float, w: Float, h: Float) {
        // Decline (red) + Answer (blue) matching reference
        p.color = 0xFFFF5252.toInt()
        c.drawCircle(w - 78f * d, 36f * d, 16f * d, p)
        p.color = 0xFF448AFF.toInt()
        c.drawCircle(w - 36f * d, 36f * d, 16f * d, p)
        p.color = Color.WHITE
        p.textSize = 14f * d
        p.textAlign = Paint.Align.CENTER
        c.drawText("☎", w - 78f * d, 41f * d, p)
        c.drawText("☎", w - 36f * d, 41f * d, p)
    }

    private fun drawProgress(c: Canvas, d: Float, w: Float, h: Float) {
        if (activity.progress < 0f) return
        p.color = 0x33FFFFFF
        c.drawRoundRect(RectF(36f * d, 52f * d, w - 36f * d, 58f * d), 3f * d, 3f * d, p)
        p.color = 0xFF69F0AE.toInt()
        val filled = 36f * d + (w - 72f * d) * activity.progress.coerceIn(0f, 1f)
        c.drawRoundRect(RectF(36f * d, 52f * d, filled, 58f * d), 3f * d, 3f * d, p)
    }
}
