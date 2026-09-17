package com.aether.launcher

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.AetherSetupView
import com.aether.launcher.system.AetherOverlayService
import com.aether.launcher.ui.AetherGlassRoot

class AetherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // REAL system wallpaper behind the launcher (works on MIUI / most OEMs)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        AetherRuntime.initialize(applicationContext)
        AetherOverlayService.ensureRunning(this)

        val store = AetherSettingsStore(this)
        if (store.load().setupComplete) {
            showHome()
        } else {
            setContentView(AetherSetupView(this) { showHome() })
        }
    }

    private fun showHome() {
        val glass = AetherGlassRoot(this)
        glass.attach(AetherHomeView(this))
        setContentView(glass)
    }

    override fun onResume() {
        super.onResume()
        AetherOverlayService.ensureRunning(this)
    }
}
