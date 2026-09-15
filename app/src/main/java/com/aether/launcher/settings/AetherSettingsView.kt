package com.aether.launcher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import com.aether.launcher.system.AetherAppLockActivity

class AetherSettingsView(context: Context) : FrameLayout(context) {
    private val store=AetherSettingsStore(context);private var s=store.load();private val density=resources.displayMetrics.density
    private val body=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(30),dp(22),dp(40))}
    init{
        val bg=ImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP;alpha=.72f;setImageDrawable(runCatching{WallpaperManager.getInstance(context).drawable}.getOrElse{ColorDrawable(0xFF10141A.toInt())});if(android.os.Build.VERSION.SDK_INT>=31)setRenderEffect(RenderEffect.createBlurEffect(18f,18f,Shader.TileMode.CLAMP))}
        addView(bg,LayoutParams(-1,-1));addView(ScrollView(context).apply{isFillViewport=true;addView(body)},LayoutParams(-1,-1));render()
    }
    private fun render(){body.removeAllViews();body.addView(TextView(context).apply{text="AETHER SETTINGS";textSize=11f;letterSpacing=.25f;setTextColor(0xAFFFFFFF.toInt());gravity=Gravity.CENTER},lp(-1,30));body.addView(TextView(context).apply{text="Make the system feel like Aether.";textSize=34f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER},lp(-1,78));body.addView(TextView(context).apply{text="Everything below is live and persisted on-device.";textSize=13f;setTextColor(0xCFFFFFFF.toInt());gravity=Gravity.CENTER},lp(-1,40));section("HOME")
        choice("Home mode","${s.homeMode}"){v->s=s.copy(homeMode=v);save()}
        choice("Grid","${s.grid.columns} × ${s.grid.rows}  •  ${if(s.grid.showLabels)"labels on"else"labels off"}"){toggleGridLabels()}
        toggle("Show app labels",s.grid.showLabels){s=s.copy(grid=s.grid.copy(showLabels=it));save()}
        toggle("Show dock",s.dock.enabled){s=s.copy(dock=s.dock.copy(enabled=it));save()}
        toggle("Smart dock grouping",s.dock.smartGroups){s=s.copy(dock=s.dock.copy(smartGroups=it));save()}
        section("LIQUID GLASS")
        value("Opacity","${s.glass.opacity}%")
        value("Blur","${s.glass.blur}px")
        value("Refraction","${s.glass.refraction}%")
        value("Depth","${s.glass.depth}dp")
        section("MOTION")
        choice("Motion preset","${s.motion.preset}"){s=s.copy(motion=s.motion.copy(preset=nextPreset(s.motion.preset)));save()}
        value("Spring","${s.motion.springStrength}")
        value("Damping","${s.motion.damping}")
        value("Gesture sensitivity","${s.motion.gestureSensitivity}%")
        section("DYNAMIC ISLAND")
        toggle("Enable Island",s.island.enabled){s=s.copy(island=s.island.copy(enabled=it));save()}
        toggle("Adapt to camera cutout",s.island.cutoutAware){s=s.copy(island=s.island.copy(cutoutAware=it));save()}
        toggle("Live notifications",s.island.showNotifications){s=s.copy(island=s.island.copy(showNotifications=it));save()}
        toggle("Live media",s.island.showMedia){s=s.copy(island=s.island.copy(showMedia=it));save()}
        toggle("Calls / timers / recording",s.island.showCalls||s.island.showTimers||s.island.showRecording){val n=s.island.copy(showCalls=it,showTimers=it,showRecording=it);s=s.copy(island=n);save()}
        section("QUICK SPACE")
        toggle("Enable Quick Space",s.quickSpace.enabled){s=s.copy(quickSpace=s.quickSpace.copy(enabled=it));save()}
        choice("Edge","${s.quickSpace.edge}"){s=s.copy(quickSpace=s.quickSpace.copy(edge=if(s.quickSpace.edge==QuickEdge.RIGHT)QuickEdge.LEFT else QuickEdge.RIGHT));save()}
        value("Trigger","${s.quickSpace.triggerDistance}px")
        section("CONTROL CENTER")
        toggle("Enable Control Center",s.controlCenter.enabled){s=s.copy(controlCenter=s.controlCenter.copy(enabled=it));save()}
        value("Tiles","${s.controlCenter.columns} columns")
        section("MULTITASKING")
        toggle("Floating window layer",s.floatingWindows.enabled){s=s.copy(floatingWindows=s.floatingWindows.copy(enabled=it));save()}
        toggle("Edge resize",s.floatingWindows.edgeResize){s=s.copy(floatingWindows=s.floatingWindows.copy(edgeResize=it));save()}
        toggle("Corner resize",s.floatingWindows.cornerResize){s=s.copy(floatingWindows=s.floatingWindows.copy(cornerResize=it));save()}
        toggle("Snap windows",s.floatingWindows.snapWindows){s=s.copy(floatingWindows=s.floatingWindows.copy(snapWindows=it));save()}
        toggle("Split screen",s.splitScreen.enabled){s=s.copy(splitScreen=s.splitScreen.copy(enabled=it));save()}
        section("SECURITY")
        toggle("App Lock",s.security.appLockEnabled){s=s.copy(security=s.security.copy(appLockEnabled=it));save()}
        toggle("Relock on leaving",s.security.relockOnLeave){s=s.copy(security=s.security.copy(relockOnLeave=it));save()}
        toggle("Relock on screen off",s.security.relockOnScreenOff){s=s.copy(security=s.security.copy(relockOnScreenOff=it));save()}
        action("Manage protected apps","Choose exactly which installed apps are protected."){context.startActivity(Intent(context,AetherAppLockActivity::class.java))}
        action("Accessibility integration","Open Android's Accessibility settings."){runCatching{context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}
        section("SYSTEM")
        action("Overlay permission","Enable the Island / edge overlay."){runCatching{context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))}}
        action("Notification Access","Enable live notification activities."){runCatching{context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}}
        action("Set Aether as Home","Choose Aether as the default launcher."){runCatching{context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))}}
        section("APPEARANCE")
        toggle("Show time",s.appearance.showTime){s=s.copy(appearance=s.appearance.copy(showTime=it));save()}
        toggle("Show Aether label",s.appearance.showAetherLabel){s=s.copy(appearance=s.appearance.copy(showAetherLabel=it));save()}
        value("Wallpaper dim","${s.appearance.wallpaperDim}%")
        body.addView(TextView(context).apply{text="AETHER • ${BuildConfig.VERSION_NAME}";textSize=11f;setTextColor(0x70FFFFFF.toInt());gravity=Gravity.CENTER;setPadding(0,dp(20),0,0)},lp(-1,40))
    }
    private fun section(t:String){body.addView(TextView(context).apply{text=t;textSize=10f;letterSpacing=.22f;setTextColor(0xAFFFFFFF.toInt());setPadding(dp(2),dp(22),dp(2),dp(7))},lp(-1,38))}
    private fun toggle(label:String,value:Boolean,change:(Boolean)->Unit){val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),0,dp(8),0);setBackgroundColor(0x38FFFFFF.toInt())};row.addView(TextView(context).apply{text=label;textSize=14f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(58),1f));row.addView(Switch(context).apply{isChecked=value;setOnCheckedChangeListener{_,v->change(v)}},LinearLayout.LayoutParams(dp(58),dp(58)));body.addView(row,lp(-1,60).also{it.setMargins(0,dp(3),0,dp(3))})}
    private fun value(label:String,detail:String){val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),0,dp(16),0);setBackgroundColor(0x2DFFFFFF.toInt())};row.addView(TextView(context).apply{text=label;textSize=14f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(52),1f));row.addView(TextView(context).apply{text=detail;textSize=12f;setTextColor(0xBFFFFFFF.toInt())});body.addView(row,lp(-1,54).also{it.setMargins(0,dp(3),0,dp(3))})}
    private fun action(label:String,detail:String,onClick:()->Unit){val row=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackgroundColor(0x3AFFFFFF.toInt());setOnClickListener{onClick()}};row.addView(TextView(context).apply{text=label;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});row.addView(TextView(context).apply{text=detail;textSize=11f;setTextColor(0xBFFFFFFF.toInt());setPadding(0,dp(4),0,0)});body.addView(row,lp(-1,-2).also{it.setMargins(0,dp(4),0,dp(4))})}
    private fun choice(label:String,detail:String,onClick:()->Unit){action(label,detail,onClick)}
    private fun toggleGridLabels(){s=s.copy(grid=s.grid.copy(showLabels=!s.grid.showLabels));save()}
    private fun save(){store.save(s);invalidate()}
    private fun nextPreset(v:MotionPreset)=when(v){MotionPreset.SOFT->MotionPreset.BALANCED;MotionPreset.BALANCED->MotionPreset.FLUID;MotionPreset.FLUID->MotionPreset.ELASTIC;MotionPreset.ELASTIC->MotionPreset.SOFT}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h)
    private fun dp(v:Int)=(v*density).toInt()
}
