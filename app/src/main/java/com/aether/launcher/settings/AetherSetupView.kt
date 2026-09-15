package com.aether.launcher.settings

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import kotlin.math.roundToInt

class AetherSetupView(context: Context, private val onFinished: () -> Unit) : ScrollView(context) {
    private val store = AetherSettingsStore(context)
    private var s = store.load()
    private val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(30), dp(24), dp(30)); setBackgroundColor(0xFF080A0E.toInt()) }
    private var page = 0
    private val titles = listOf("Welcome","Home","Grid","Appearance","Dock","Dynamic Island","Quick Space","Control Center","Multitasking","Security","Ready")
    init { setFillViewport(true); addView(root); render() }

    private fun render() {
        root.removeAllViews()
        root.addView(TextView(context).apply { text="AETHER  •  ${page+1}/${titles.size}"; textSize=11f; setTextColor(0x99FFFFFF.toInt()) }, lp(-1,34))
        root.addView(TextView(context).apply { text=titles[page]; textSize=30f; setTextColor(Color.WHITE); typeface=android.graphics.Typeface.DEFAULT_BOLD; setPadding(0,dp(12),0,dp(8)) }, lp(-1,-2))
        root.addView(TextView(context).apply { text=subtitle(); textSize=14f; setTextColor(0xBFFFFFFF.toInt()); setPadding(0,0,0,dp(18)) }, lp(-1,-2))
        when(page){
            0 -> root.addView(note("A calm Android home with real apps, live widgets, Quick Space, Dynamic Island and system-backed controls."))
            1 -> choice("Home mode", HomeMode.values().toList(), s.homeMode) { s=s.copy(homeMode=it);save() }
            2 -> { seek("Columns",3,7,s.grid.columns){s=s.copy(grid=s.grid.copy(columns=it));save()}; seek("Rows",4,9,s.grid.rows){s=s.copy(grid=s.grid.copy(rows=it));save()}; seek("Icon size",40,72,s.grid.iconSize){s=s.copy(grid=s.grid.copy(iconSize=it));save()}; toggle("App labels",s.grid.showLabels){s=s.copy(grid=s.grid.copy(showLabels=it));save()} }
            3 -> { choice("Theme",AppearanceTheme.values().toList(),s.appearance.theme){s=s.copy(appearance=s.appearance.copy(theme=it));save()}; seek("Glass opacity",45,100,s.glass.opacity){s=s.copy(glass=s.glass.copy(opacity=it));save()}; seek("Glass blur",0,50,s.glass.blur){s=s.copy(glass=s.glass.copy(blur=it));save()}; seek("Corner radius",8,42,s.glass.cornerRadius){s=s.copy(glass=s.glass.copy(cornerRadius=it));save()}; choice("Motion",MotionPreset.values().toList(),s.motion.preset){s=s.copy(motion=s.motion.copy(preset=it));save()} }
            4 -> { toggle("Dock enabled",s.dock.enabled){s=s.copy(dock=s.dock.copy(enabled=it));save()}; seek("Dock favorites",3,8,s.dock.appCount){s=s.copy(dock=s.dock.copy(appCount=it));save()}; seek("Dock height",56,110,s.dock.height){s=s.copy(dock=s.dock.copy(height=it));save()} }
            5 -> { toggle("Dynamic Island",s.island.enabled){s=s.copy(island=s.island.copy(enabled=it));save()}; toggle("Cutout aware",s.island.cutoutAware){s=s.copy(island=s.island.copy(cutoutAware=it));save()}; seek("Island width",72,180,s.island.width){s=s.copy(island=s.island.copy(width=it));save()} }
            6 -> { toggle("Quick Space",s.quickSpace.enabled){s=s.copy(quickSpace=s.quickSpace.copy(enabled=it));save()}; choice("Edge",QuickEdge.values().toList(),s.quickSpace.edge){s=s.copy(quickSpace=s.quickSpace.copy(edge=it));save()}; seek("Trigger distance",40,180,s.quickSpace.triggerDistance){s=s.copy(quickSpace=s.quickSpace.copy(triggerDistance=it));save()} }
            7 -> { toggle("Control Center",s.controlCenter.enabled){s=s.copy(controlCenter=s.controlCenter.copy(enabled=it));save()}; seek("Tile columns",1,4,s.controlCenter.columns){s=s.copy(controlCenter=s.controlCenter.copy(columns=it));save()}; toggle("Brightness",s.controlCenter.showBrightness){s=s.copy(controlCenter=s.controlCenter.copy(showBrightness=it));save()}; toggle("Wi-Fi",s.controlCenter.showWifi){s=s.copy(controlCenter=s.controlCenter.copy(showWifi=it));save()} }
            8 -> { toggle("Floating windows",s.floatingWindows.enabled){s=s.copy(floatingWindows=s.floatingWindows.copy(enabled=it));save()}; toggle("Split screen",s.splitScreen.enabled){s=s.copy(splitScreen=s.splitScreen.copy(enabled=it));save()}; seek("Split ratio",30,70,s.splitScreen.defaultRatio){s=s.copy(splitScreen=s.splitScreen.copy(defaultRatio=it));save()} }
            9 -> { toggle("App Lock",s.security.appLockEnabled){s=s.copy(security=s.security.copy(appLockEnabled=it));save()}; toggle("Relock when leaving",s.security.relockOnLeave){s=s.copy(security=s.security.copy(relockOnLeave=it));save()}; toggle("Use biometrics",s.security.useBiometrics){s=s.copy(security=s.security.copy(useBiometrics=it));save()} }
            10 -> root.addView(note("Your choices are saved. After setup, every section remains editable in Aether Settings."))
        }
        val nav=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(22),0,0)}
        if(page>0) nav.addView(button("Back"){page--;render()},lp(0,56,1f).also{it.marginEnd=dp(10)})
        nav.addView(button(if(page==titles.lastIndex)"Enter Aether" else "Continue"){if(page==titles.lastIndex){store.save(s.copy(setupComplete=true));onFinished()}else{page++;render()}},lp(0,56,1f))
        root.addView(nav)
    }

    private fun save(){store.save(s)}
    private fun subtitle()=when(page){0->"Set up your launcher once. Everything stays editable.";1->"Choose the home model that fits you.";2->"Tune density without hiding real installed apps.";3->"Shape the visual material and motion.";4->"Choose how many favorite apps live at the bottom.";5->"Tune the live activity surface.";6->"Make your edge shortcut behave naturally.";7->"Configure system-backed quick controls.";8->"Use Android's supported multi-window behavior.";9->"Protect the apps you care about.";else->"Aether is ready."}
    private fun note(text:String)=TextView(context).apply{text=text;textSize=16f;setTextColor(0xDFFFFFFF.toInt());setPadding(dp(20),dp(24),dp(20),dp(24));setBackgroundColor(0x181F2933)}
    private fun button(label:String,action:()->Unit)=Button(context).apply{text=label;setTextColor(Color.WHITE);textSize=14f;setOnClickListener{action()}}
    private fun toggle(label:String,value:Boolean,action:(Boolean)->Unit){val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(TextView(context).apply{text=label;textSize=15f;setTextColor(Color.WHITE)},lp(0,56,1f));row.addView(Switch(context).apply{isChecked=value;setOnCheckedChangeListener{_,v->action(v)}},lp(70,52));root.addView(row)}
    private fun seek(label:String,min:Int,max:Int,value:Int,action:(Int)->Unit){val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(8),0,dp(4))};val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL};val v=TextView(context).apply{text=value.toString();textSize=12f;setTextColor(0xBFFFFFFF.toInt());gravity=Gravity.END};row.addView(TextView(context).apply{text=label;textSize=15f;setTextColor(Color.WHITE)},lp(0,30,1f));row.addView(v,lp(56,30));val bar=SeekBar(context).apply{this.max=max-min;progress=(value-min).coerceIn(0,this.max);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(sb:SeekBar?,p:Int,fromUser:Boolean){val n=p+min;v.text=n.toString();if(fromUser)action(n)};override fun onStartTrackingTouch(sb:SeekBar?){ };override fun onStopTrackingTouch(sb:SeekBar?){ }})};box.addView(row);box.addView(bar);root.addView(box)}
    private fun <E:Enum<E>> choice(label:String,values:List<E>,selected:E,action:(E)->Unit){val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL};row.addView(TextView(context).apply{text=label;textSize=15f;setTextColor(Color.WHITE)},lp(0,56,1f));val spinner=Spinner(context);spinner.adapter=ArrayAdapter(context,android.R.layout.simple_spinner_dropdown_item,values.map{it.name.replace('_',' ').lowercase().replaceFirstChar{c->c.uppercase()}});spinner.setSelection(values.indexOf(selected));spinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onItemSelected(p:AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){action(values[pos])};override fun onNothingSelected(p:AdapterView<*>?){}};row.addView(spinner,lp(190,56));root.addView(row)}
    private fun lp(w:Int,h:Int,weight:Float=0f)=LinearLayout.LayoutParams(if(w==0)0 else dp(w),if(h<0)-1 else dp(h),weight)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
}
