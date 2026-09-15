package com.aether.launcher.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.aether.launcher.AetherRuntime

class AetherOverlayService : Service() {
    private var islandOverlay: AetherIslandOverlay? = null

    override fun onCreate() {
        super.onCreate()
        val channelId = "aether_core"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "Aether Core", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Aether is active")
            .setContentText("Glass interaction and security layer running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)
        runCatching {
            AetherRuntime.initialize(applicationContext)
            islandOverlay = AetherIslandOverlay(this).also { it.show() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { islandOverlay?.refresh() }
        return START_STICKY
    }

    override fun onDestroy() {
        islandOverlay?.hide()
        islandOverlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
