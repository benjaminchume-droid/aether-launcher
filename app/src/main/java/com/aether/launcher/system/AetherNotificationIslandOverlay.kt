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
 * System-wide Dynamic Island.
 * Always attached when overlay is granted. Sits in/near the camera cutout.
 * Resizes for media / call / download / notification / charging.
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
        if (!c.enabled) return

        val v = CapsuleView(service)
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val p = WindowManager.LayoutParams(
            dp(c.width.coerceAtLeast(110)),
            dp(c.height.coerceAtLeast(30)),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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

    /** Prefer the physical cutout; expand width to cover it when cutoutAware. */
    private fun place(p: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = wm.currentWindowMetrics.windowInsets
            val top = i.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            val cutout = i.displayCutout
            val cut = cutout?.boundingRects?.maxByOrNull { it.width() * it.height() }
            val cfg = store.load().island
            if (cut != null && cfg.cutoutAware) {
                // Sit just under the cutout, width at least cutout + padding
                p.y = max(0, cut.bottom - dp(2))
                p.width = max(p.width, cut.width() + dp(64))
            } else {
                p.y = top + dp(cfg.topOffset.coerceIn(0, 24))
            }
        } else {
            val id = service.resources.getIdentifier("status_bar_height", "dimen", "android")
            val status = if (id != 0) service.resources.getDimensionPixelSize(id) else dp(24)
            p.y = status + dp(4)
        }
    }

    fun refresh() {
        val v = view ?: return
        if (!Settings.canDrawOverlays(service)) return

        val a = AetherRuntime.registry.island.activity
        if (a.key != key && key.isNotBlank()) expanded = false
        key = a.key
        v.activity = a
        v.replyable = a.type == ActivityType.NOTIFICATION && NotificationReplyBridge.canReply(a.key)

        val active = a.type != ActivityType.NONE && a.title.isNotBlank()
        if (!active) expanded = false

        val c = store.load().island
        val w = when {
            expanded && a.type == ActivityType.MEDIA -> dp(320)
            expanded && a.type == ActivityType.CALL -> dp(300)
            expanded && a.type == ActivityType.DOWNLOAD -> dp(300)
            expanded && a.type == ActivityType.NOTIFICATION -> dp(320)
            expanded && active -> dp(280)
            active -> dp(max(c.width, 120))
            else -> dp(max(c.width, 110)) // idle capsule still visible
        }
        val h = when {
            expanded && a.type == ActivityType.NOTIFICATION && v.replyable -> dp(168)
            expanded && a.type == ActivityType.MEDIA -> dp(96)
            expanded && a.type == ActivityType.CALL -> dp(78)
            expanded && a.type == ActivityType.DOWNLOAD -> dp(72)
            expanded && active -> dp(80)
            else -> dp(c.height.coerceAtLeast(30))
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
        if (a.type == ActivityType.NONE) {
            // Idle tap opens notification surface
            AetherSystemActions.openSurface(service, com.aether.launcher.ui.AetherSurface.NOTIFICATIONS)
            return
        }
        expanded = !expanded
        lp?.let {
            it.flags = if (expanded)
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            else
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
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
        setBackgroundColor(Color.TRANSPARENT)
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
        input = EditText(context).apply {
            hint = "Reply\u2026"
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
            text = "\u2192"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            background = bg(0xFF1E88FF.toInt(), 26)
            setOnClickListener { sendReply() }
        }
        addView(input, LayoutParams(-1, dp(48)).apply {
            leftMargin = dp(14); rightMargin = dp(66); topMargin = dp(108)
        })
        addView(send, LayoutParams(dp(48), dp(48)).apply {
            gravity = android.view.Gravity.END
            rightMargin = dp(14); topMargin = dp(108)
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
        val dens = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(h / 2f, 30f * dens)

        // Deep glass
        p.color = 0xF40A0C10.toInt()
        canvas.drawRoundRect(RectF(0.5f, 0.5f, w - 0.5f, h - 0.5f), r, r, p)

        p.shader = LinearGradient(
            0f, 0f, 0f, h * 0.5f,
            0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), r, r, p)
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, dens)
        p.color = 0x66FFFFFF.toInt()
        canvas.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), r, r, p)
        p.style = Paint.Style.FILL

        val expanded = h > 55f * dens
        val active = activity.type != ActivityType.NONE && activity.title.isNotBlank()

        when (activity.type) {
            ActivityType.RECORDING, ActivityType.CALL -> {
                p.color = if (activity.type == ActivityType.RECORDING) 0xFFFF5252.toInt() else 0xFF69F0AE.toInt()
                canvas.drawCircle(20f * dens, min(h * 0.5f, 18f * dens), 4f * dens, p)
            }
            ActivityType.DOWNLOAD -> {
                p.color = 0xFF69F0AE.toInt()
                canvas.drawCircle(20f * dens, min(h * 0.5f, 18f * dens), 4f * dens, p)
            }
            ActivityType.MEDIA -> {
                p.color = 0xFFEA80FC.toInt()
                canvas.drawCircle(20f * dens, min(h * 0.5f, 18f * dens), 4f * dens, p)
            }
            ActivityType.CHARGING -> {
                p.color = 0xFF8BE28A.toInt()
                canvas.drawCircle(20f * dens, min(h * 0.5f, 18f * dens), 4f * dens, p)
            }
            else -> {
                p.color = 0x88FFFFFF.toInt()
                canvas.drawCircle(20f * dens, min(h * 0.5f, 18f * dens), 3f * dens, p)
            }
        }

        p.color = 0xF8FFFFFF.toInt()
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = (if (expanded) 13f else 11f) * dens
        p.textAlign = Paint.Align.LEFT
        val title = if (active) activity.title.take(if (expanded) 34 else 20) else "AETHER"
        canvas.drawText(title, 32f * dens, min(h * 0.55f, 20f * dens), p)

        if (!expanded) return

        p.typeface = Typeface.DEFAULT
        p.textSize = 11f * dens
        p.color = 0xB8FFFFFF.toInt()
        if (activity.detail.isNotBlank()) {
            canvas.drawText(activity.detail.take(40), 32f * dens, 40f * dens, p)
        }

        when (activity.type) {
            ActivityType.MEDIA -> {
                if (activity.progress in 0f..1f) {
                    p.color = 0x33FFFFFF
                    canvas.drawRoundRect(RectF(32f * dens, 52f * dens, w - 32f * dens, 56f * dens), 2f * dens, 2f * dens, p)
                    p.color = 0xFFEA80FC.toInt()
                    val filled = 32f * dens + (w - 64f * dens) * activity.progress
                    canvas.drawRoundRect(RectF(32f * dens, 52f * dens, filled, 56f * dens), 2f * dens, 2f * dens, p)
                }
                p.color = Color.WHITE
                p.textSize = 18f * dens
                p.textAlign = Paint.Align.CENTER
                canvas.drawText("\u23EE  \u23F8  \u23ED", w / 2f, 80f * dens, p)
            }
            ActivityType.CALL -> {
                p.color = 0xFFFF5252.toInt()
                canvas.drawCircle(w - 72f * dens, 36f * dens, 15f * dens, p)
                p.color = 0xFF448AFF.toInt()
                canvas.drawCircle(w - 32f * dens, 36f * dens, 15f * dens, p)
            }
            ActivityType.DOWNLOAD, ActivityType.CHARGING -> {
                if (activity.progress >= 0f) {
                    p.color = 0x33FFFFFF
                    canvas.drawRoundRect(RectF(32f * dens, 52f * dens, w - 32f * dens, 58f * dens), 3f * dens, 3f * dens, p)
                    p.color = 0xFF69F0AE.toInt()
                    val filled = 32f * dens + (w - 64f * dens) * activity.progress.coerceIn(0f, 1f)
                    canvas.drawRoundRect(RectF(32f * dens, 52f * dens, filled, 58f * dens), 3f * dens, 3f * dens, p)
                }
            }
            else -> {}
        }
    }
}
