package com.aether.launcher.system

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType
import com.aether.launcher.engine.island.IslandActivity
import com.aether.launcher.settings.AetherSettingsStore
import kotlin.math.max
import kotlin.math.min

class AetherNotificationIslandOverlay(private val service:AetherOverlayService){
 private val wm=service.getSystemService(WindowManager::class.java);private val store=AetherSettingsStore(service);private var view:CapsuleView?=null;private var lp:WindowManager.LayoutParams?=null;private var expanded=false;private var key="";private val d get()=service.resources.displayMetrics.density;private fun dp(v:Int)=(v*d).toInt()
 fun show(){if(view!=null||!Settings.canDrawOverlays(service))return;val c=store.load().island;val v=CapsuleView(service);val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE;val p=WindowManager.LayoutParams(dp(c.width.coerceAtLeast(92)),dp(c.height.coerceAtLeast(28)),type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,android.graphics.PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE};place(p);runCatching{wm.addView(v,p);view=v;lp=p;v.setOnClickListener{toggle()};v.setOnLongClickListener{service.startActivity(android.content.Intent(service,NotificationSettingsActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));true};refresh()}}
 private fun place(p:WindowManager.LayoutParams){if(Build.VERSION.SDK_INT>=30){val i=wm.currentWindowMetrics.windowInsets;val top=i.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;val cut=i.displayCutout?.boundingRects?.maxByOrNull{it.width()*it.height()};p.y=if(cut!=null&&store.load().island.cutoutAware)max(top,cut.bottom)+dp(3)else top+dp(store.load().island.topOffset);if(cut!=null&&store.load().island.cutoutAware)p.width=max(p.width,cut.width()+dp(54))}else p.y=dp(store.load().island.topOffset+24)}
 fun refresh(){val v=view?:return;val a=AetherRuntime.registry.island.activity;if(a.key!=key&&key.isNotBlank())expanded=false;key=a.key;v.activity=a;v.replyable=a.type==ActivityType.NOTIFICATION&&NotificationReplyBridge.canReply(a.key);val active=a.type!=ActivityType.NONE&&a.title.isNotBlank();if(!active)expanded=false;val c=store.load().island;resize(if(expanded&&active)dp(min(380,max(c.width+100,270)))else dp(c.width.coerceAtLeast(92)),if(expanded&&a.type==ActivityType.NOTIFICATION)dp(176)else if(expanded&&active)dp(max(c.height+54,84))else dp(c.height.coerceAtLeast(28)))}
 private fun resize(w:Int,h:Int){val p=lp?:return;val v=view?:return;p.width=w;p.height=h;place(p);runCatching{wm.updateViewLayout(v,p)}}
 private fun toggle(){val a=AetherRuntime.registry.island.activity;if(a.type==ActivityType.NONE)return;expanded=!expanded;lp?.let{it.flags=if(expanded)WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;runCatching{wm.updateViewLayout(view,it)}};refresh();if(expanded&&a.type==ActivityType.NOTIFICATION&&NotificationReplyBridge.canReply(a.key))view?.postDelayed({view?.focusReply()},120)}
 fun hide(){view?.let{runCatching{wm.removeView(it)}};view=null;lp=null}
}

private class CapsuleView(c:Context):FrameLayout(c){var activity=IslandActivity(ActivityType.NONE,"");var replyable=false;private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var input:EditText?=null;private var send:TextView?=null;private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();init{setWillNotDraw(false)}
 fun focusReply(){input?.requestFocus();(context.getSystemService(Context.INPUT_METHOD_SERVICE)as?InputMethodManager)?.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT)}
 override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int){super.onSizeChanged(w,h,ow,oh);if(replyable&&h>100f*resources.displayMetrics.density)composer()else clearComposer()}
 private fun composer(){if(input!=null)return;input=EditText(context).apply{hint="Reply…";setTextColor(Color.WHITE);setHintTextColor(0x99FFFFFF.toInt());textSize=15f;setSingleLine(true);setPadding(dp(14),0,dp(14),0);background=bg(0xE62A2E34.toInt(),28);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEND;setOnEditorActionListener{_,id,_->if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEND){sendReply();true}else false}};send=TextView(context).apply{text="➜";textSize=22f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=bg(0xFF168BFF.toInt(),28);setOnClickListener{sendReply()}};addView(input,LayoutParams(-1,dp(48)).apply{leftMargin=dp(14);rightMargin=dp(64);topMargin=dp(112)});addView(send,LayoutParams(dp(48),dp(48)).apply{gravity=Gravity.RIGHT;rightMargin=dp(14);topMargin=dp(112)})}
 private fun clearComposer(){input?.let{removeView(it)};send?.let{removeView(it)};input=null;send=null}
 private fun bg(c:Int,r:Int)=android.graphics.drawable.GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
 private fun sendReply(){if(NotificationReplyBridge.reply(activity.key,input?.text?.toString().orEmpty())){input?.setText("");(context.getSystemService(Context.INPUT_METHOD_SERVICE)as?InputMethodManager)?.hideSoftInputFromWindow(windowToken,0)}}
 override fun onDraw(c:Canvas){val d=resources.displayMetrics.density;val w=width.toFloat();val h=height.toFloat();val r=min(h/2f,30f*d);p.color=0xF30A0C10.toInt();c.drawRoundRect(RectF(.5f,.5f,w-.5f,h-.5f),r,r,p);p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,d);p.color=0x50FFFFFF;c.drawRoundRect(RectF(1f,1f,w-1f,h-1f),r,r,p);p.style=Paint.Style.FILL;val ex=h>55f*d;val active=activity.type!=ActivityType.NONE&&activity.title.isNotBlank();p.color=Color.WHITE;p.textSize=(if(ex)12 else 10)*d;p.typeface=android.graphics.Typeface.DEFAULT_BOLD;p.textAlign=Paint.Align.LEFT;c.drawText(if(active)activity.title.take(if(ex)36 else 24)else "AETHER",38f*d,min(h*.54f,23f*d),p);if(!ex)return;p.typeface=android.graphics.Typeface.DEFAULT;p.textSize=9f*d;p.color=0xBFFFFFFF.toInt();if(activity.detail.isNotBlank())c.drawText(activity.detail.take(48),38f*d,42f*d,p);p.textSize=10f*d;c.drawText(if(replyable)"Quick reply • stays until notification changes"else "Tap to open activity",38f*d,74f*d,p)}}
