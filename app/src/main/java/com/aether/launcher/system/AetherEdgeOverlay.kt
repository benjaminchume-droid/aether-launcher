package com.aether.launcher.system

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.QuickEdge
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.GlassPainter
import kotlin.math.max

/**
 * System-wide Quick Space edge handle + glass panel.
 * Available over any app (overlay permission required).
 * Matches the liquid-glass language used everywhere else.
 */
class AetherEdgeOverlay(private val service: AetherOverlayService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val store = AetherSettingsStore(service)
    private var root: FrameLayout? = null
    private var panel: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private val d get() = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    fun show() {
        if (root != null || !Settings.canDrawOverlays(service)) return
        val cfg = store.load().quickSpace
        if (!cfg.enabled) return

        val frame = FrameLayout(service).apply {
            clipChildren = false
            clipToPadding = false
        }

        val handle = object : View(service) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(c: Canvas) {
                val r = RectF(2f * d, height * 0.35f, width - 2f * d, height * 0.35f + cfg.handleLength * d)
                GlassPainter.drawPill(c, r, 0xE0181C24.toInt())
            }
            override fun onTouchEvent(e: MotionEvent): Boolean {
                if (e.actionMasked == MotionEvent.ACTION_UP) toggle()
                return true
            }
        }

        val edgeGravity = if (cfg.edge == QuickEdge.LEFT)
            Gravity.CENTER_VERTICAL or Gravity.START
        else
            Gravity.CENTER_VERTICAL or Gravity.END

        frame.addView(
            handle,
            FrameLayout.LayoutParams(dp(cfg.handleThickness.coerceIn(4, 10) + 8), dp(cfg.handleLength.coerceIn(36, 80) + 20), edgeGravity).apply {
                if (cfg.edge == QuickEdge.RIGHT) rightMargin = dp(2) else leftMargin = dp(2)
            }
        )

        root = frame
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            dp(28),
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (cfg.edge == QuickEdge.LEFT)
                Gravity.START or Gravity.CENTER_VERTICAL
            else
                Gravity.END or Gravity.CENTER_VERTICAL
        }

        params = lp
        runCatching { wm.addView(frame, lp) }
    }

    private fun toggle() {
        val r = root ?: return
        val lp = params ?: return
        val cfg = store.load().quickSpace

        if (panel != null) {
            r.removeView(panel)
            panel = null
            lp.width = dp(28)
            runCatching { wm.updateViewLayout(r, lp) }
            return
        }

        val p = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(16))
            background = GradientDrawable().apply {
                setColor(0xF012161E.toInt())
                cornerRadius = 26f * d
                setStroke(dp(1), 0x55FFFFFF)
            }
            elevation = 22f * d
        }

        val tools = buildList {
            if (cfg.notes) add("Notes" to { AetherSystemActions.openSurface(service, AetherSurface.QUICK) })
            if (cfg.recorder) add("Recorder" to { AetherSystemActions.openSurface(service, AetherSurface.QUICK) })
            if (cfg.calculator) add("Calculator" to { AetherSystemActions.openSurface(service, AetherSurface.QUICK) })
            if (cfg.screenshot) add("Screenshot" to { /* real capture later */ })
            if (cfg.clipboard) add("Clipboard" to { AetherSystemActions.openSurface(service, AetherSurface.QUICK) })
            if (cfg.recentApps) add("Recents" to { AetherSystemActions.openSurface(service, AetherSurface.HISTORY) })
            add("Control Center" to { AetherSystemActions.openSurface(service, AetherSurface.CONTROL) })
            add("Settings" to { AetherSystemActions.openSettings(service) })
            add("Home" to { AetherSystemActions.launchHome(service) })
        }

        tools.forEach { (label, action) ->
            p.addView(
                TextView(service).apply {
                    text = label
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(18), 0, dp(18), 0)
                    background = GradientDrawable().apply {
                        setColor(0x33181C24)
                        cornerRadius = 18f * d
                    }
                    setOnClickListener {
                        action()
                        // auto-close after action
                        toggle()
                    }
                },
                LinearLayout.LayoutParams(dp(220), dp(48)).apply {
                    bottomMargin = dp(8)
                }
            )
        }

        val gravity = if (cfg.edge == QuickEdge.LEFT)
            Gravity.START or Gravity.CENTER_VERTICAL
        else
            Gravity.END or Gravity.CENTER_VERTICAL

        r.addView(
            p,
            FrameLayout.LayoutParams(dp(248), max(dp(8), r.height - dp(120)), gravity).apply {
                if (cfg.edge == QuickEdge.RIGHT) rightMargin = dp(36) else leftMargin = dp(36)
            }
        )

        panel = p
        lp.width = dp(280)
        runCatching { wm.updateViewLayout(r, lp) }

        p.translationX = if (cfg.edge == QuickEdge.RIGHT) dp(40).toFloat() else -dp(40).toFloat()
        SpringAnimation(p, DynamicAnimation.TRANSLATION_X).apply {
            spring = SpringForce(0f).apply {
                stiffness = 520f
                dampingRatio = 0.78f
            }
        }.start()
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        panel = null
        params = null
    }
}
