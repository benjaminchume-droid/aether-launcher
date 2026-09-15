package com.aether.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity

class AetherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AetherRuntime.initialize(this)
        setContentView(AetherView(this))
    }

    override fun onDestroy() {
        if (isFinishing) AetherRuntime.registry.stopAll()
        super.onDestroy()
    }
}
