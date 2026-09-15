package com.aether.launcher.system

import android.app.Service
import android.content.Intent
import android.os.IBinder
class AetherOverlayService:Service(){override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{ return START_STICKY };override fun onBind(intent:Intent?):IBinder?=null}
