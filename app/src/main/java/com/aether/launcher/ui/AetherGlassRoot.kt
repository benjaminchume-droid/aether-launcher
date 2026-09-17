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
 * Wallpaper-first root.
 * Prefers FLAG_SHOW_WALLPAPER from the activity; also draws WallpaperManager drawable as fallback.
 */
class AetherGlassRoot(context: android.content.Context) : FrameLayout(context) {

    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 1f
        setImageDrawable(
            runCatching { WallpaperManager.getInstance(context).drawable }
                .getOrNull()
        )
        // If drawable is null, stay transparent so FLAG_SHOW_WALLPAPER shows through
        if (drawable == null) {
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private val atmosphere = View(context).apply {
        // Very light dim so glass/icons read without killing wallpaper
        setBackgroundColor(0x22000000)
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(wallpaper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(atmosphere, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        if (Build.VERSION.SDK_INT >= 31 && wallpaper.drawable != null) {
            // Soft blur only when we have a drawable layer
            wallpaper.setRenderEffect(
                RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP)
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
                text = "\u2039"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
                setOnClickListener { (context as? Activity)?.finish() }
            }
            addView(back, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = 48
                leftMargin = 8
            })
        }
    }
}
