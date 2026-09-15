package com.aether.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.aether.launcher.settings.AetherSettingsStore
import com.aether.launcher.settings.AetherSetupView
import com.aether.launcher.ui.AetherGlassRoot

class AetherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        AetherRuntime.initialize(applicationContext)
        val store=AetherSettingsStore(this)
        if(store.load().setupComplete){
            val glass=AetherGlassRoot(this)
            glass.attach(AetherHomeView(this))
            setContentView(glass)
        }else{
            setContentView(AetherSetupView(this){
                val glass=AetherGlassRoot(this)
                glass.attach(AetherHomeView(this))
                setContentView(glass)
            })
        }
    }
}
