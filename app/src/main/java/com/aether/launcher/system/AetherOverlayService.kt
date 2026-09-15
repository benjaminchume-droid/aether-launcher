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
    override fun onCreate() {
        super.onCreate()
        val channelId="aether_core"
        if(Build.VERSION.SDK_INT>=26){getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channelId,"Aether Core",NotificationManager.IMPORTANCE_LOW))}
        val notification=Notification.Builder(this,channelId).setContentTitle("Aether is active").setContentText("Glass interaction and security layer running").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build()
        startForeground(1001,notification)
        if(!::safeRuntimeReady) { try { AetherRuntime.initialize(applicationContext); safeRuntimeReady=true } catch(_:Throwable){} }
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int = START_STICKY
    override fun onBind(intent:Intent?):IBinder?=null
    companion object { private var safeRuntimeReady=false }
}
