package com.aether.launcher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.settings.AetherSettingsStore

/**
 * Foreground-app observer for App Lock and launcher history.
 *
 * App Lock deliberately uses Android's user-enabled Accessibility API. Android does
 * not permit a normal launcher to silently observe foreground apps without that
 * explicit special-access grant.
 */
class AetherAccessibilityService : AccessibilityService() {
    private lateinit var history: AetherHistoryStore
    private lateinit var settings: AetherSettingsStore
    private var lastPackage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        AetherRuntime.initialize(applicationContext)
        history = AetherHistoryStore(applicationContext)
        settings = AetherSettingsStore(applicationContext)
        AetherRuntime.sensing.onAppChanged { transition ->
            val pkg = transition.packageName
            if (pkg.isBlank() || pkg == "android") return@onAppChanged

            if (pkg != packageName) history.record(pkg)

            // Leaving a protected app ends its unlock session when configured.
            val previous = lastPackage
            if (previous.isNotBlank() && previous != pkg &&
                AetherRuntime.security.isProtected(previous) &&
                settings.load().security.relockOnLeave
            ) {
                AetherRuntime.security.lock(previous)
            }
            lastPackage = pkg

            if (pkg != packageName &&
                settings.load().security.appLockEnabled &&
                AetherRuntime.security.isProtected(pkg) &&
                !AetherRuntime.security.isUnlocked(pkg)
            ) {
                SecurityOverlayController.show(this, pkg)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { AetherRuntime.sensing.publish(it) }
        }
    }

    override fun onInterrupt() {
        // Nothing to release; app-lock sessions are managed by foreground transitions.
    }
}
