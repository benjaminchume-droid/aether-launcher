package com.aether.launcher.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.aether.launcher.AetherRuntime

class AetherOverlayService : Service() {
    private var islandOverlay: AetherIslandOverlay? = null
    private var edgeOverlay: AetherEdgeOverlay? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            runCatching { islandOverlay?.refresh() }
            handler.postDelayed(this, 220L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "aether_core"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "Aether Core", NotificationManager.IMPORTANCE_LOW)
            )
        }
        startForeground(
            1001,
            Notification.Builder(this, channelId)
                .setContentTitle("Aether is active")
                .setContentText("System interaction layer running")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        )
        runCatching {
            AetherRuntime.initialize(applicationContext)
            islandOverlay = AetherIslandOverlay(this).also { it.show() }
            edgeOverlay = AetherEdgeOverlay(this).also { it.show() }
            handler.post(refresher)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { islandOverlay?.refresh() }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        islandOverlay?.hide()
        edgeOverlay?.hide()
        islandOverlay = null
        edgeOverlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
