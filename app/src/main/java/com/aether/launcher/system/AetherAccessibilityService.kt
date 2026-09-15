package com.aether.launcher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aether.launcher.AetherRuntime

class AetherAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        if (!::runtimeReady) runtimeReady = true
        AetherRuntime.initialize(applicationContext)
        AetherRuntime.sensing.onAppChanged { transition ->
            if (AetherRuntime.security.isProtected(transition.packageName) && !AetherRuntime.security.isUnlocked(transition.packageName)) {
                SecurityOverlayController.show(this, transition.packageName)
            }
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { AetherRuntime.sensing.publish(it) }
        }
    }
    override fun onInterrupt() {}
    companion object { private var runtimeReady = false }
}
