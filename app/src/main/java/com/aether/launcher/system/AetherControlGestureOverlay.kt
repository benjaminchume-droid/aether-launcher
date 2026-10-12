package com.aether.launcher.system

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.ui.AetherSurface
import com.aether.launcher.ui.AetherSurfaceActivity
import kotlin.math.abs

/**
 * Small top-edge gesture target for Aether's custom Control Center.
 * The Android notification shade remains available from gestures that begin
 * outside this narrow target; the overlay cannot replace privileged SystemUI.
 */
class AetherControlGestureOverlay(private val service: AetherOverlayService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private var target: View? = null
    private val density get() = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    fun show() {
        if (target != null || !android.provider.Settings.canDrawOverlays(service)) return
        if (!AetherSettingsStore(service).load().controlCenter.enabled) return

        val view = object : View(service) {
            private var startX = 0f
            private var startY = 0f
            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dy = event.rawY - startY
                        if (dy > dp(42) && abs(event.rawX - startX) < dp(140)) {
                            runCatching {
                                service.startActivity(
                                    Intent(service, AetherSurfaceActivity::class.java)
                                        .putExtra("surface", AetherSurface.CONTROL)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                )
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return true
            }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(12),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        runCatching {
            wm.addView(view, lp)
            target = view
        }
    }

    fun hide() {
        target?.let { runCatching { wm.removeView(it) } }
        target = null
    }
}
