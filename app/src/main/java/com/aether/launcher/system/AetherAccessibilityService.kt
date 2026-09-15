package com.aether.launcher.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.aether.launcher.engine.sensing.AppSensingEngine
class AetherAccessibilityService:AccessibilityService(){ override fun onAccessibilityEvent(event:AccessibilityEvent?){if(event?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){ val pkg=event.packageName?.toString() ?: return; AetherRuntime.sensing.publish(pkg)}} override fun onInterrupt(){} }
object AetherRuntime { lateinit var sensing:AppSensingEngine }
