package com.aether.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.AetherSetupView

class AetherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AetherRuntime.initialize(applicationContext)
        val store = AetherSettingsStore(this)
        if (store.load().setupComplete) {
            setContentView(AetherHomeView(this))
        } else {
            setContentView(AetherSetupView(this) { setContentView(AetherHomeView(this)) })
        }
    }
}
