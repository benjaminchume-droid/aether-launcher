package com.aether.launcher.ui

import android.app.WallpaperManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader

/** Wallpaper-backed material root. Blur is performed by Android's RenderEffect on API 31+. */
class AetherGlassRoot(context: Context) : FrameLayout(context) {
    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = .82f
        setImageDrawable(runCatching { WallpaperManager.getInstance(context).drawable }.getOrElse { ColorDrawable(0xFF0B0D11.toInt()) })
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP))
        }
    }
    init {
        addView(wallpaper, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    fun attach(content: android.view.View) { addView(content, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) }
}
