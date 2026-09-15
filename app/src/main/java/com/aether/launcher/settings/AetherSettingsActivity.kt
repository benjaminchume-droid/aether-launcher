package com.aether.launcher.settings

import android.os.Bundle
import androidx.activity.ComponentActivity

class AetherSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(AetherSettingsView(this))
    }
}
