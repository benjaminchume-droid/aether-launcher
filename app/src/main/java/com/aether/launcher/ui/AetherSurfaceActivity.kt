package com.aether.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.aether.launcher.AetherRuntime

/** Surfaces (Quick Space, Drawer, Control, etc.) sit on real wallpaper. */
class AetherSurfaceActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        AetherRuntime.initialize(applicationContext)
        val root = AetherGlassRoot(this)
        root.attach(
            AetherSurfaceView(
                this,
                intent.getStringExtra("surface") ?: AetherSurface.DRAWER,
                intent.getStringExtra("folder_id")
            )
        )
        setContentView(root)
    }
}
