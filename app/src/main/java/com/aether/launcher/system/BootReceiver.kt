package com.aether.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AetherOverlayService::class.java)
                )
                AetherRuntime.initialize(context.applicationContext)
                val battery = context.getSystemService(BatteryManager::class.java)
                if (battery?.isCharging == true) {
                    AetherRuntime.registry.island.setActivity(
                        IslandActivity(
                            type = ActivityType.CHARGING,
                            title = "Charging",
                            detail = "Power connected",
                            priority = 20,
                            key = "power"
                        )
                    )
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                AetherRuntime.initialize(context.applicationContext)
                AetherRuntime.registry.island.setActivity(
                    IslandActivity(
                        type = ActivityType.CHARGING,
                        title = "Charging",
                        detail = "Power connected",
                        priority = 20,
                        key = "power"
                    )
                )
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                if (AetherRuntime.isInitialized()) {
                    AetherRuntime.registry.island.clearActivity("power")
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                if (AetherRuntime.isInitialized()) {
                    AetherRuntime.security.lockAll()
                }
            }
        }
    }
}
