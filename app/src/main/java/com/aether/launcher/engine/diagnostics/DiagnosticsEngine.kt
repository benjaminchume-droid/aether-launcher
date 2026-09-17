package com.aether.launcher.engine.diagnostics

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

data class DiagnosticLine(val label: String, val value: String, val ok: Boolean = true)

/**
 * Phase 4 diagnostics surface — real permission + engine health, no invented status.
 */
class DiagnosticsEngine(private val context: Context? = null) : AetherEngine {
    override val id = "diagnostics"
    private var state = EngineHealth.STOPPED

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED }
    override fun health() = state

    fun snapshot(): List<DiagnosticLine> {
        val ctx = context ?: return listOf(DiagnosticLine("Context", "missing", false))
        val lines = mutableListOf<DiagnosticLine>()

        lines += DiagnosticLine("Android", "API ${Build.VERSION.SDK_INT}")
        lines += DiagnosticLine(
            "Overlay", 
            if (Settings.canDrawOverlays(ctx)) "granted" else "missing",
            Settings.canDrawOverlays(ctx)
        )

        if (AetherRuntime.isInitialized()) {
            AetherRuntime.registry.engines.forEach { e ->
                val h = e.health()
                lines += DiagnosticLine(e.id, h.name, h == EngineHealth.RUNNING)
            }
            val perf = AetherRuntime.registry.performance.profile()
            lines += DiagnosticLine("Performance", perf.tier.name)
            lines += DiagnosticLine("Blur", if (perf.blurEnabled) "on (${perf.blurRadius.toInt()})" else "off")
            lines += DiagnosticLine("Protected apps", AetherRuntime.security.protectedApps().size.toString())
            lines += DiagnosticLine("PIN set", if (AetherRuntime.security.hasPin()) "yes" else "no")
            lines += DiagnosticLine("Island", AetherRuntime.registry.island.activity.type.name)
            lines += DiagnosticLine("Notifications", AetherRuntime.registry.notifications.all().size.toString())
        } else {
            lines += DiagnosticLine("Runtime", "not initialized", false)
        }
        return lines
    }
}
