package com.aether.launcher.settings

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.aether.launcher.ui.AetherGlassRoot

class AetherSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = AetherSettingsView(this)
        settings.getChildAt(0)?.setBackgroundColor(Color.TRANSPARENT)
        val glass = AetherGlassRoot(this)
        glass.attach(settings)
        setContentView(glass)
    }
}
