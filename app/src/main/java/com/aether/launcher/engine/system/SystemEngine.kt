package com.aether.launcher.engine.system

import android.content.Context
import com.aether.launcher.engine.*
enum class Capability { OVERLAY, ACCESSIBILITY, NOTIFICATIONS, SHIZUKU, DEVICE_POLICY }
class SystemBridge(private val context:Context) { fun hasOverlay()=android.provider.Settings.canDrawOverlays(context); fun isShizukuAvailable():Boolean=try{Class.forName("rikka.shizuku.Shizuku"); true}catch(_:Throwable){false} }
class SystemEngine: AetherEngine { override val id="system"; private var state=EngineHealth.STOPPED; override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state }
