package com.aether.launcher.settings

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import com.aether.launcher.AetherRuntime
import com.aether.launcher.system.AetherAppLockActivity

/** Full visual control surface for Aether. */
class AetherSettingsView(context: Context) : FrameLayout(context) {
    private val store=AetherSettingsStore(context); private var s=store.load(); private val d get()=resources.displayMetrics.density
    private val body=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(34),dp(22),dp(44))}

    init{addView(ScrollView(context).apply{isFillViewport=true;addView(body)},LayoutParams(-1,-1));render()}

    private fun render(){
        body.removeAllViews()
        label("A E T H E R  /  SETTINGS")
        title("Your system.\nYour motion.")
        sub("Every control below is persisted locally. Nothing is a fake preview.")

        section("HOME")
        action("Home behavior",s.homeMode.name.replace('_',' ')){s=s.copy(homeMode=nextHome(s.homeMode));save();render()}
        action("Grid density","${s.grid.columns} × ${s.grid.rows}  •  ${s.grid.iconSize}dp icons"){s=s.copy(grid=s.grid.copy(columns=nextGrid(s.grid.columns),rows=nextRows(s.grid.rows)));save();render()}
        toggle("App labels",s.grid.showLabels){s=s.copy(grid=s.grid.copy(showLabels=it));save()}
        toggle("Glass dock",s.dock.enabled){s=s.copy(dock=s.dock.copy(enabled=it));save()}
        action("Dock apps","${s.dock.appCount} favorites  •  long-press on Home to move apps"){}

        section("LIQUID GLASS")
        slider("Opacity",s.glass.opacity,55,100){s=s.copy(glass=s.glass.copy(opacity=it));save()}
        slider("Blur",s.glass.blur,0,40){s=s.copy(glass=s.glass.copy(blur=it));save()}
        slider("Refraction",s.glass.refraction,0,40){s=s.copy(glass=s.glass.copy(refraction=it));save()}
        slider("Depth",s.glass.depth,2,18){s=s.copy(glass=s.glass.copy(depth=it));save()}

        section("MOTION")
        action("Preset",s.motion.preset.name){s=s.copy(motion=s.motion.copy(preset=nextPreset(s.motion.preset)));save();render()}
        slider("Speed",s.motion.speed,60,160){s=s.copy(motion=s.motion.copy(speed=it));save()}
        slider("Spring",s.motion.springStrength,220,700){s=s.copy(motion=s.motion.copy(springStrength=it));save()}
        slider("Damping",s.motion.damping,16,60){s=s.copy(motion=s.motion.copy(damping=it));save()}

        section("DYNAMIC ISLAND")
        toggle("Island",s.island.enabled){s=s.copy(island=s.island.copy(enabled=it));save()}
        toggle("Camera-cutout aware",s.island.cutoutAware){s=s.copy(island=s.island.copy(cutoutAware=it));save()}
        toggle("Notifications",s.island.showNotifications){s=s.copy(island=s.island.copy(showNotifications=it));save()}
        toggle("Media / calls / timers",s.island.showMedia||s.island.showCalls||s.island.showTimers){s=s.copy(island=s.island.copy(showMedia=it,showCalls=it,showTimers=it));save()}
        action("Notification Access","Connect Android notifications to the Island"){open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)}

        section("QUICK SPACE")
        toggle("Quick Space",s.quickSpace.enabled){s=s.copy(quickSpace=s.quickSpace.copy(enabled=it));save()}
        action("Edge",s.quickSpace.edge.name){s=s.copy(quickSpace=s.quickSpace.copy(edge=if(s.quickSpace.edge==QuickEdge.RIGHT)QuickEdge.LEFT else QuickEdge.RIGHT));save();render()}
        toggle("Notes",s.quickSpace.notes){s=s.copy(quickSpace=s.quickSpace.copy(notes=it));save()}
        toggle("Voice recorder",s.quickSpace.recorder){s=s.copy(quickSpace=s.quickSpace.copy(recorder=it));save()}
        toggle("Screenshot",s.quickSpace.screenshot){s=s.copy(quickSpace=s.quickSpace.copy(screenshot=it));save()}

        section("MULTITASKING")
        toggle("Floating window layer",s.floatingWindows.enabled){s=s.copy(floatingWindows=s.floatingWindows.copy(enabled=it));save()}
        toggle("Edge resize",s.floatingWindows.edgeResize){s=s.copy(floatingWindows=s.floatingWindows.copy(edgeResize=it));save()}
        toggle("Corner resize",s.floatingWindows.cornerResize){s=s.copy(floatingWindows=s.floatingWindows.copy(cornerResize=it));save()}
        toggle("Snap windows",s.floatingWindows.snapWindows){s=s.copy(floatingWindows=s.floatingWindows.copy(snapWindows=it));save()}
        toggle("Split screen",s.splitScreen.enabled){s=s.copy(splitScreen=s.splitScreen.copy(enabled=it));save()}

        section("SECURITY")
        toggle("App Lock",s.security.appLockEnabled){s=s.copy(security=s.security.copy(appLockEnabled=it));save()}
        toggle("Relock when leaving",s.security.relockOnLeave){s=s.copy(security=s.security.copy(relockOnLeave=it));save()}
        toggle("Relock on screen off",s.security.relockOnScreenOff){s=s.copy(security=s.security.copy(relockOnScreenOff=it));save()}
        action("Protected apps","Choose exactly which apps require biometric / device credential"){context.startActivity(Intent(context,AetherAppLockActivity::class.java))}
        action("Accessibility integration","Enable foreground sensing for App Lock"){open(Settings.ACTION_ACCESSIBILITY_SETTINGS)}

        section("SYSTEM")
        action("Default Home","Choose Aether as the Android Home role"){open(Settings.ACTION_HOME_SETTINGS)}
        action("Overlay permission","Allow Island and edge surfaces above apps"){runCatching{context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:${context.packageName}")))}}
        action("Notification Access","Live activities and notification surfaces"){open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)}
        action("App widgets","Open Aether's widget host"){runCatching{context.startActivity(Intent(context,com.aether.launcher.ui.AetherWidgetHostActivity::class.java))}}

        section("APPEARANCE")
        toggle("Show time",s.appearance.showTime){s=s.copy(appearance=s.appearance.copy(showTime=it));save()}
        toggle("Show Aether label",s.appearance.showAetherLabel){s=s.copy(appearance=s.appearance.copy(showAetherLabel=it));save()}
        slider("Wallpaper dim",s.appearance.wallpaperDim,0,30){s=s.copy(appearance=s.appearance.copy(wallpaperDim=it));save()}
        label("AETHER  •  0.5.0  •  LIQUID SYSTEM")
    }

    private fun label(text:String){body.addView(TextView(context).apply{this.text=text;textSize=10f;letterSpacing=.25f;setTextColor(0x9FFFFFFF.toInt());setPadding(0,dp(8),0,dp(8))},lp(-1,34))}
    private fun title(text:String){body.addView(TextView(context).apply{this.text=text;textSize=36f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);setShadowLayer(18f,0f,5f,0x70000000)},lp(-1,94))}
    private fun sub(text:String){body.addView(TextView(context).apply{this.text=text;textSize=13f;setTextColor(0xCFFFFFFF.toInt());setLineSpacing(4f,1f)},lp(-1,54))}
    private fun section(text:String){body.addView(TextView(context).apply{this.text=text;textSize=10f;letterSpacing=.22f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0x9FFFFFFF.toInt());setPadding(dp(3),dp(22),dp(3),dp(8))},lp(-1,46))}
    private fun cardBase():LinearLayout=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(10),dp(10));background=android.graphics.drawable.GradientDrawable().apply{setColor(0x68101821);cornerRadius=dp(22).toFloat();setStroke(dp(1),0x3DFFFFFF)}}
    private fun toggle(name:String,value:Boolean,on:(Boolean)->Unit){val row=cardBase();row.addView(TextView(context).apply{text=name;textSize=14f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(56),1f));row.addView(Switch(context).apply{isChecked=value;setOnCheckedChangeListener{_,v->on(v)}},LinearLayout.LayoutParams(dp(58),dp(56)));body.addView(row,lp(-1,62).also{it.setMargins(0,dp(3),0,dp(3))})}
    private fun action(name:String,detail:String,on:()->Unit){val row=cardBase();row.orientation=LinearLayout.VERTICAL;row.gravity=Gravity.CENTER_VERTICAL;row.setOnClickListener{on()};row.addView(TextView(context).apply{text=name;textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});row.addView(TextView(context).apply{text=detail.ifBlank{"Tap to change"};textSize=10f;setTextColor(0xBFFFFFFF.toInt());setPadding(0,dp(4),0,0)});body.addView(row,lp(-1,64).also{it.setMargins(0,dp(3),0,dp(3))})}
    private fun slider(name:String,value:Int,min:Int,max:Int,on:(Int)->Unit){val box=cardBase();box.orientation=LinearLayout.VERTICAL;val top=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL};top.addView(TextView(context).apply{text=name;textSize=13f;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(30),1f));top.addView(TextView(context).apply{text=value.toString();textSize=11f;setTextColor(0xBFFFFFFF.toInt())},LinearLayout.LayoutParams(dp(48),dp(30)));box.addView(top);box.addView(SeekBar(context).apply{this.max=max-min;progress=value-min;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar?,p:Int,f:Boolean){if(f)on(p+min)};override fun onStartTrackingTouch(b:SeekBar?){};override fun onStopTrackingTouch(b:SeekBar?){} })},LinearLayout.LayoutParams(-1,dp(30)));body.addView(box,lp(-1,76).also{it.setMargins(0,dp(3),0,dp(3))})}
    private fun save(){store.save(s)}
    private fun nextHome(v:HomeMode)=when(v){HomeMode.ALL_APPS->HomeMode.HOME_AND_DRAWER;HomeMode.HOME_AND_DRAWER->HomeMode.DRAWER_ONLY;HomeMode.DRAWER_ONLY->HomeMode.ALL_APPS}
    private fun nextGrid(v:Int)=when(v){4->5;5->6;6->4;else->4};private fun nextRows(v:Int)=when(v){4->5;5->6;6->4;else->6}
    private fun nextPreset(v:MotionPreset)=when(v){MotionPreset.SOFT->MotionPreset.BALANCED;MotionPreset.BALANCED->MotionPreset.FLUID;MotionPreset.FLUID->MotionPreset.ELASTIC;MotionPreset.ELASTIC->MotionPreset.SOFT}
    private fun open(action:String){runCatching{context.startActivity(Intent(action))}}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h);private fun dp(v:Int)=(v*d).toInt()
}
