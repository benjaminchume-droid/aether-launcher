package com.aether.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.aether.launcher.AetherRuntime

/** All secondary surfaces (Quick, Drawer, Control, Settings path) on real wallpaper. */
class AetherSurfaceActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        AetherRuntime.initialize(applicationContext)
        val root = AetherGlassRoot(this)
        val surface = intent.getStringExtra("surface") ?: AetherSurface.DRAWER
        val content = if (surface == AetherSurface.DRAWER) AetherAppDrawerView(this) else AetherSurfaceView(this, surface, intent.getStringExtra("folder_id"))
        root.attach(content)
        setContentView(root)
    }
}
