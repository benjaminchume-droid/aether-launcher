package com.aether.launcher.system

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.ui.AetherSurface

class AetherEdgeOverlay(private val service: AetherOverlayService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private var root: LinearLayout? = null
    private var panel: LinearLayout? = null
    private fun dp(v:Int)=(v*service.resources.displayMetrics.density).toInt()
    fun show(){
        if(root!=null||!android.provider.Settings.canDrawOverlays(service))return
        val r=LinearLayout(service).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(6),0,dp(6),0);setBackgroundColor(Color.TRANSPARENT)}
        val handle=TextView(service).apply{text="☰";gravity=Gravity.CENTER;textSize=16f;setTextColor(Color.WHITE);setBackgroundColor(0xCC171A20.toInt());setOnClickListener{toggle()}}
        r.addView(handle,LinearLayout.LayoutParams(dp(44),dp(52)))
        root=r
        val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        wm.addView(r,WindowManager.LayoutParams(dp(56),WindowManager.LayoutParams.MATCH_PARENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.CENTER_VERTICAL or Gravity.RIGHT})
    }
    private fun toggle(){if(panel!=null){panel=null;root?.let{it.removeViews(1,it.childCount-1)};return};val p=LinearLayout(service).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(8));setBackgroundColor(0xEE12151A.toInt())};val items=listOf("Home","History","Notifications","Control Center","Widgets","Multitask","Settings");items.forEach{label->p.addView(TextView(service).apply{text=label;textSize=13f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),0,dp(14),0);setOnClickListener{when(label){"Home"->AetherSystemActions.launchHome(service);"History"->AetherSystemActions.openSurface(service,AetherSurface.HISTORY);"Notifications"->AetherSystemActions.openSurface(service,AetherSurface.NOTIFICATIONS);"Control Center"->AetherSystemActions.openSurface(service,AetherSurface.CONTROL);"Widgets"->AetherSystemActions.openSurface(service,AetherSurface.WIDGETS);"Multitask"->AetherSystemActions.openSurface(service,AetherSurface.MULTITASK);"Settings"->AetherSystemActions.openSettings(service)}}},LinearLayout.LayoutParams(dp(210),dp(48))) };root?.addView(p);panel=p}
    fun hide(){root?.let{runCatching{wm.removeView(it)}};root=null;panel=null}
}
