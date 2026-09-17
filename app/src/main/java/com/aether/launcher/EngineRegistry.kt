package com.aether.launcher

import android.content.Context
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.capture.AudioCaptureEngine
import com.aether.launcher.engine.control.ControlCenterEngine
import com.aether.launcher.engine.glass.GlassEngine
import com.aether.launcher.engine.island.DynamicIslandEngine
import com.aether.launcher.engine.launcher.LauncherEngine
import com.aether.launcher.engine.multitasking.MultitaskingEngine
import com.aether.launcher.engine.notes.NotesEngine
import com.aether.launcher.engine.notification.NotificationEngine
import com.aether.launcher.engine.overlay.OverlayEngine
import com.aether.launcher.engine.performance.PerformanceEngine
import com.aether.launcher.engine.persistence.PersistenceEngine
import com.aether.launcher.engine.physics.PhysicsEngine
import com.aether.launcher.engine.security.SecurityEngine
import com.aether.launcher.engine.sensing.AppSensingEngine
import com.aether.launcher.engine.sensing.StripingSensingEngine
import com.aether.launcher.engine.system.SystemEngine

class EngineRegistry(context: Context) {
    val glass = GlassEngine()
    val physics = PhysicsEngine()
    val overlay = OverlayEngine()
    val security = SecurityEngine(context)
    val sensing = AppSensingEngine()
    val striping = StripingSensingEngine()
    val island = DynamicIslandEngine()
    val multitasking = MultitaskingEngine(context)
    val notifications = NotificationEngine()
    val system = SystemEngine()
    val persistence = PersistenceEngine(context)
    val launcher = LauncherEngine(context)
    val controlCenter = ControlCenterEngine(context)
    val audioCapture = AudioCaptureEngine()
    val notes = NotesEngine()
    val performance = PerformanceEngine(context)

    val engines: List<AetherEngine> = listOf(
        glass, physics, overlay, security, sensing, striping, island, multitasking,
        notifications, system, persistence, launcher, controlCenter, audioCapture, notes, performance
    )

    fun startAll() = engines.forEach { it.start() }
    fun stopAll() = engines.asReversed().forEach { it.stop() }
}
