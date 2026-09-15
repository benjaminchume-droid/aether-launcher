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

/** Wallpaper-backed Aether material root with a consistent secondary-surface back control. */
class AetherGlassRoot(context: android.content.Context) : FrameLayout(context) {
    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = .88f
        setImageDrawable(runCatching { WallpaperManager.getInstance(context).drawable }.getOrElse { ColorDrawable(0xFF0B0D11.toInt()) })
        if (android.os.Build.VERSION.SDK_INT >= 31) setRenderEffect(RenderEffect.createBlurEffect(28f,28f,Shader.TileMode.CLAMP))
    }
    init { addView(wallpaper, LayoutParams(-1,-1)) }
    fun attach(content: View) {
        addView(content, LayoutParams(-1,-1))
        if (content is AetherSurfaceView) {
            val back = TextView(context).apply {
                text = "‹"
                textSize = 34f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(0x66181B21)
                elevation = 18f
                setOnClickListener { (context as? Activity)?.finish() }
                contentDescription = "Back"
            }
            val size = (52 * resources.displayMetrics.density).toInt()
            val lp = LayoutParams(size,size).apply { gravity = Gravity.TOP or Gravity.START; leftMargin=(14*resources.displayMetrics.density).toInt(); topMargin=(18*resources.displayMetrics.density).toInt() }
            addView(back,lp)
        }
    }
}
