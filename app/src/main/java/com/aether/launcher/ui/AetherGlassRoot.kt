package com.aether.launcher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowCompat

/** Shared Aether visual root. Wallpaper is the material underneath every surface. */
class AetherGlassRoot(context: android.content.Context) : FrameLayout(context) {
    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = .82f
        setImageDrawable(runCatching { WallpaperManager.getInstance(context).drawable }.getOrElse { ColorDrawable(0xFF0B0F15.toInt()) })
    }
    private val atmosphere = View(context).apply {
        setBackgroundColor(0x21050A11)
    }
    private val vignette = View(context).apply {
        setBackgroundColor(0x16000000)
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(wallpaper, LayoutParams(-1, -1))
        addView(atmosphere, LayoutParams(-1, -1))
        addView(vignette, LayoutParams(-1, -1))
        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        (context as? Activity)?.let { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= 29) {
                activity.window.isStatusBarContrastEnforced = false
                activity.window.isNavigationBarContrastEnforced = false
            }
        }
    }

    fun attach(content: View) {
        addView(content, LayoutParams(-1, -1))
        if (content is AetherSurfaceView) {
            val back = TextView(context).apply {
                text = "‹"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x45151B24)
                    cornerRadius = 22f * resources.displayMetrics.density
                    setStroke((resources.displayMetrics.density).toInt().coerceAtLeast(1), 0x45FFFFFF)
                }
                elevation = 18f * resources.displayMetrics.density
                setOnClickListener { (context as? Activity)?.finish() }
                contentDescription = "Back"
            }
            val size = (46 * resources.displayMetrics.density).toInt()
            addView(back, LayoutParams(size, size).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = (12 * resources.displayMetrics.density).toInt()
                topMargin = (18 * resources.displayMetrics.density).toInt()
            })
        }
    }
}
