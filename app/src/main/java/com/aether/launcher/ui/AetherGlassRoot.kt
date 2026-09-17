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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.aether.launcher.AetherRuntime

/**
 * Shared visual root for every Aether surface.
 * Wallpaper is the material underneath. Real blur applied when supported.
 * Refraction atmosphere is always present.
 */
class AetherGlassRoot(context: android.content.Context) : FrameLayout(context) {

    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.88f
        setImageDrawable(
            runCatching { WallpaperManager.getInstance(context).drawable }
                .getOrElse { ColorDrawable(0xFF0B0F15.toInt()) }
        )
    }

    private val atmosphere = View(context).apply {
        // Soft dark atmosphere so glass surfaces pop
        setBackgroundColor(0x28050A11)
    }

    private val vignette = View(context).apply {
        setBackgroundColor(0x12000000)
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(wallpaper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(atmosphere, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(vignette, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Real blur on the wallpaper layer when available
        if (Build.VERSION.SDK_INT >= 31 && AetherRuntime.isInitialized()) {
            val radius = AetherRuntime.registry.glass.material().blurRadius.coerceIn(8f, 40f)
            wallpaper.setRenderEffect(
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        }

        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

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
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        if (content is AetherSurfaceView) {
            val back = TextView(context).apply {
                text = "‹"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x66151B24)
                    cornerRadius = 22f * resources.displayMetrics.density
                    setStroke(
                        (resources.displayMetrics.density).toInt().coerceAtLeast(1),
                        0x55FFFFFF
                    )
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
