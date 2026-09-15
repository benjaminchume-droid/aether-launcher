package com.aether.launcher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime

class AetherAccessibilityService:AccessibilityService(){
    private lateinit var history:AetherHistoryStore
    override fun onServiceConnected(){
        AetherRuntime.initialize(applicationContext)
        history=AetherHistoryStore(applicationContext)
        AetherRuntime.sensing.onAppChanged { transition ->
            if(transition.packageName!=packageName&&transition.packageName!="android"&&transition.packageName.isNotBlank()) history.record(transition.packageName)
            if(AetherRuntime.security.isProtected(transition.packageName)&&!AetherRuntime.security.isUnlocked(transition.packageName)) SecurityOverlayController.show(this,transition.packageName)
        }
    }
    override fun onAccessibilityEvent(event:AccessibilityEvent?){if(event?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){event.packageName?.toString()?.let{AetherRuntime.sensing.publish(it)}}}
    override fun onInterrupt(){}
}
