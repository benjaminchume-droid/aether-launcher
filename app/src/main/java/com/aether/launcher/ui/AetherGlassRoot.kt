package com.aether.launcher.ui

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Shared visual root: wallpaper remains visible underneath every Aether surface. */
class AetherGlassRoot(context: android.content.Context) : FrameLayout(context) {
    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = .72f
        setImageDrawable(runCatching { WallpaperManager.getInstance(context).drawable }.getOrElse { ColorDrawable(0xFF10141B.toInt()) })
        if (android.os.Build.VERSION.SDK_INT >= 31) setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP))
    }
    private val atmosphere = View(context).apply {
        setBackgroundColor(0x1F05070B)
        alpha = .9f
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(wallpaper, LayoutParams(-1, -1))
        addView(atmosphere, LayoutParams(-1, -1))
        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    fun attach(content: View) {
        addView(content, LayoutParams(-1, -1))
        if (content is AetherSurfaceView) {
            val back = TextView(context).apply {
                text = "‹"
                textSize = 30f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(0x351A1E26)
                elevation = 18f
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
