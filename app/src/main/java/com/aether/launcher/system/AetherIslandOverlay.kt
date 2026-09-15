package com.aether.launcher.system

import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.aether.launcher.AetherRuntime

/** Lightweight Dynamic Island overlay. Requires the user to grant Draw over other apps. */
class AetherIslandOverlay(private val service: AetherOverlayService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private var view: View? = null

    fun show() {
        if (view != null || !android.provider.Settings.canDrawOverlays(service)) return
        val text = TextView(service).apply {
            text = "AETHER"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            gravity = Gravity.CENTER
            setBackgroundColor(0xE915171B.toInt())
            setPadding(28, 8, 28, 8)
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 10
        }
        wm.addView(text, lp)
        view = text
    }

    fun refresh() {
        val v = view as? TextView ?: return
        val activity = AetherRuntime.registry.island.activity
        v.text = if (activity.title.isBlank()) "AETHER" else activity.title.take(28)
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }
}
