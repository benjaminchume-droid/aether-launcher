package com.aether.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action==Intent.ACTION_BOOT_COMPLETED||intent.action==Intent.ACTION_LOCKED_BOOT_COMPLETED){context.startService(Intent(context,AetherOverlayService::class.java))}}}
