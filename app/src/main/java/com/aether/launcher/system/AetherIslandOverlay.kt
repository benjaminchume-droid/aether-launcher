package com.aether.launcher.system

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.aether.launcher.AetherRuntime
import com.aether.launcher.settings.AetherSettingsStore

class AetherIslandOverlay(private val service:AetherOverlayService){
    private val wm=service.getSystemService(WindowManager::class.java)
    private val store=AetherSettingsStore(service)
    private var view:TextView?=null
    private var lp:WindowManager.LayoutParams?=null
    private val density get()=service.resources.displayMetrics.density
    private fun dp(v:Int)= (v*density).toInt()
    fun show(){
        if(view!=null||!android.provider.Settings.canDrawOverlays(service))return
        val cfg=store.load().island
        val text=TextView(service).apply{setTextColor(Color.WHITE);textSize=11f;gravity=Gravity.CENTER;setPadding(dp(16),dp(5),dp(16),dp(5));background=GradientDrawable().apply{setColor(0xE81A1D23.toInt());cornerRadius=dp(cfg.radius).toFloat();setStroke(dp(1),0x38FFFFFF)}}
        val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params=WindowManager.LayoutParams(dp(cfg.width),dp(cfg.height),type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT)
        params.gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL
        position(params,cfg.topOffset)
        wm.addView(text,params);view=text;lp=params
        text.scaleX=.86f;text.scaleY=.86f;SpringAnimation(text,DynamicAnimation.SCALE_X).apply{spring=SpringForce(1f).apply{stiffness=600f;dampingRatio=.72f}}.start();SpringAnimation(text,DynamicAnimation.SCALE_Y).apply{spring=SpringForce(1f).apply{stiffness=600f;dampingRatio=.72f}}.start()
        refresh()
    }
    private fun position(params:WindowManager.LayoutParams,offsetDp:Int){
        val inset=runCatching{wm.currentWindowMetrics.windowInsets}.getOrNull()
        val cutout=inset?.displayCutout
        val topInset=inset?.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())?.top?:0
        val cutoutRect=cutout?.boundingRects?.maxByOrNull{it.width()*it.height()}
        params.y=if(cutoutRect!=null)maxOf(topInset,cutoutRect.bottom)+dp(2) else topInset+dp(offsetDp)
        val cfg=store.load().island
        if(cutoutRect!=null&&cfg.cutoutAware){params.width=maxOf(params.width,cutoutRect.width()+dp(44))}
    }
    fun refresh(){val v=view?:return;val a=AetherRuntime.registry.island.activity;v.text=if(a.title.isBlank())"AETHER" else a.title.take(32);lp?.let{runCatching{position(it,store.load().island.topOffset);wm.updateViewLayout(v,it)}}}
    fun hide(){view?.let{runCatching{wm.removeView(it)}};view=null;lp=null}
}
