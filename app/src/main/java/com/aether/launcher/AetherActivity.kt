package com.aether.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.AetherSetupView
import com.aether.launcher.system.AetherOverlayService
import com.aether.launcher.ui.AetherGlassRoot

class AetherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        AetherRuntime.initialize(applicationContext)

        // Ensure Island + Quick Space are alive system-wide
        AetherOverlayService.ensureRunning(this)

        val store = AetherSettingsStore(this)
        if (store.load().setupComplete) {
            val glass = AetherGlassRoot(this)
            glass.attach(AetherHomeView(this))
            setContentView(glass)
        } else {
            setContentView(AetherSetupView(this) {
                val glass = AetherGlassRoot(this)
                glass.attach(AetherHomeView(this))
                setContentView(glass)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        AetherOverlayService.ensureRunning(this)
    }
}
