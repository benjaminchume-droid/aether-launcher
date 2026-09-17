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
import android.provider.Settings
import com.aether.launcher.AetherRuntime

/**
 * Keeps Island + Quick Space edge alive over every app.
 */
class AetherOverlayService : Service() {

    private var islandOverlay: AetherNotificationIslandOverlay? = null
    private var edgeOverlay: AetherEdgeOverlay? = null
    private val handler = Handler(Looper.getMainLooper())

    private val refresher = object : Runnable {
        override fun run() {
            runCatching {
                if (islandOverlay == null && Settings.canDrawOverlays(this@AetherOverlayService)) {
                    islandOverlay = AetherNotificationIslandOverlay(this@AetherOverlayService).also { it.show() }
                }
                if (edgeOverlay == null && Settings.canDrawOverlays(this@AetherOverlayService)) {
                    edgeOverlay = AetherEdgeOverlay(this@AetherOverlayService).also { it.show() }
                }
                islandOverlay?.refresh()
            }
            val delay = if (AetherRuntime.isInitialized()) {
                AetherRuntime.registry.performance.profile().maxOverlayRefreshMs
            } else 220L
            handler.postDelayed(this, delay)
        }
    }

    companion object {
        @Volatile private var current: AetherOverlayService? = null

        fun refreshFromSystem() {
            current?.handler?.post { current?.islandOverlay?.refresh() }
        }

        fun ensureRunning(context: android.content.Context) {
            val intent = Intent(context, AetherOverlayService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
                else context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        current = this

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
                .setContentText("Island + Quick Space")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        )

        runCatching {
            AetherRuntime.initialize(applicationContext)
            if (Settings.canDrawOverlays(this)) {
                islandOverlay = AetherNotificationIslandOverlay(this).also { it.show() }
                edgeOverlay = AetherEdgeOverlay(this).also { it.show() }
            }
            MediaSessionBridge.start(applicationContext)
            handler.post(refresher)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching {
            if (Settings.canDrawOverlays(this)) {
                if (islandOverlay == null) islandOverlay = AetherNotificationIslandOverlay(this).also { it.show() }
                if (edgeOverlay == null) edgeOverlay = AetherEdgeOverlay(this).also { it.show() }
                islandOverlay?.refresh()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        current = null
        handler.removeCallbacksAndMessages(null)
        MediaSessionBridge.stop(applicationContext)
        islandOverlay?.hide()
        edgeOverlay?.hide()
        islandOverlay = null
        edgeOverlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
