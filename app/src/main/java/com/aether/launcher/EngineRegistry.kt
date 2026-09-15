package com.aether.launcher

import android.content.Context
import com.aether.launcher.engine.*
import com.aether.launcher.engine.glass.GlassEngine
import com.aether.launcher.engine.physics.PhysicsEngine
import com.aether.launcher.engine.overlay.OverlayEngine
import com.aether.launcher.engine.security.SecurityEngine
import com.aether.launcher.engine.sensing.AppSensingEngine
import com.aether.launcher.engine.island.DynamicIslandEngine
import com.aether.launcher.engine.multitasking.MultitaskingEngine
import com.aether.launcher.engine.notification.NotificationEngine
import com.aether.launcher.engine.system.SystemEngine
import com.aether.launcher.engine.persistence.PersistenceEngine
import com.aether.launcher.engine.launcher.LauncherEngine
import com.aether.launcher.engine.control.ControlCenterEngine
import com.aether.launcher.engine.capture.AudioCaptureEngine
import com.aether.launcher.engine.notes.NotesEngine

class EngineRegistry(context:Context) { val engines:List<AetherEngine> = listOf(GlassEngine(),PhysicsEngine(),OverlayEngine(),SecurityEngine(),AppSensingEngine(),DynamicIslandEngine(),MultitaskingEngine(),NotificationEngine(),SystemEngine(),PersistenceEngine(context),LauncherEngine(context),ControlCenterEngine(),AudioCaptureEngine(),NotesEngine()); fun startAll(){engines.forEach{it.start()}}; fun stopAll(){engines.asReversed().forEach{it.stop()}} }
