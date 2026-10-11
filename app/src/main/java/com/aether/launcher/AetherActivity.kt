package com.aether.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.net.Uri
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

    @Deprecated("Use the Activity Result API when migrating this launcher entry point")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7412 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            getSharedPreferences("aether_appearance", MODE_PRIVATE)
                .edit().putString("wallpaper_uri", uri.toString()).apply()
            showHome()
        }
    }

    override fun onResume() {
        super.onResume()
        AetherOverlayService.ensureRunning(this)
    }
}
