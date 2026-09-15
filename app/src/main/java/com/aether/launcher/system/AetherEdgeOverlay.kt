package com.aether.launcher.system

import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.aether.launcher.ui.AetherSurface
import kotlin.math.max

class AetherEdgeOverlay(private val service:AetherOverlayService){
    private val wm=service.getSystemService(WindowManager::class.java)
    private var root:FrameLayout?=null
    private var menu:LinearLayout?=null
    private val d get()=service.resources.displayMetrics.density
    private fun dp(v:Int)=(v*d).toInt()
    fun show(){
        if(root!=null||!android.provider.Settings.canDrawOverlays(service))return
        val frame=FrameLayout(service).apply{clipChildren=false;clipToPadding=false}
        val handle=TextView(service).apply{text="☰";gravity=Gravity.CENTER;textSize=16f;setTextColor(Color.WHITE);setBackgroundColor(0xD9181B21.toInt());elevation=dp(8).toFloat();setOnClickListener{toggle()}}
        frame.addView(handle,FrameLayout.LayoutParams(dp(48),dp(54),Gravity.CENTER_VERTICAL or Gravity.RIGHT).apply{rightMargin=dp(4)})
        root=frame
        val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        wm.addView(frame,WindowManager.LayoutParams(dp(300),WindowManager.LayoutParams.MATCH_PARENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.RIGHT or Gravity.CENTER_VERTICAL})
    }
    private fun toggle(){val r=root?:return;if(menu!=null){r.removeView(menu);menu=null;return};val p=LinearLayout(service).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(0xEF12151A.toInt());elevation=dp(18).toFloat()};val items=listOf("Home" to {AetherSystemActions.launchHome(service)},"History" to {AetherSystemActions.openSurface(service,AetherSurface.HISTORY)},"Notifications" to {AetherSystemActions.openSurface(service,AetherSurface.NOTIFICATIONS)},"Control Center" to {AetherSystemActions.openSurface(service,AetherSurface.CONTROL)},"Widgets" to {AetherSystemActions.openSurface(service,AetherSurface.WIDGETS)},"Multitask" to {AetherSystemActions.openSurface(service,AetherSurface.MULTITASK)},"Quick Space" to {AetherSystemActions.openSurface(service,AetherSurface.QUICK)},"Settings" to {AetherSystemActions.openSettings(service)});items.forEach{(label,action)->p.addView(TextView(service).apply{text=label;textSize=14f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),0,dp(16),0);setOnClickListener{action()}},LinearLayout.LayoutParams(dp(248),dp(52)).apply{bottomMargin=dp(6)})};r.addView(p,FrameLayout.LayoutParams(dp(268),max(dp(8),r.height-dp(80)),Gravity.RIGHT or Gravity.TOP).apply{topMargin=dp(24);rightMargin=dp(58)});menu=p}
    fun hide(){root?.let{runCatching{wm.removeView(it)}};root=null;menu=null}
}
