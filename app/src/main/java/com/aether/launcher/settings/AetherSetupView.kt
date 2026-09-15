package com.aether.launcher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.aether.launcher.AetherRuntime

/** First-run setup: visual, short and useful instead of a form dump. */
class AetherSetupView(context: Context, private val onFinished: () -> Unit) : FrameLayout(context) {
    private val store=AetherSettingsStore(context)
    private var s=store.load()
    private var page=0
    private val d get()=resources.displayMetrics.density
    private val body=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(24),dp(28),dp(24),dp(32))}
    private val wallpaper=ImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP;alpha=.62f;setImageDrawable(runCatching{WallpaperManager.getInstance(context).drawable}.getOrElse{ColorDrawable(0xFF10141A.toInt())})}

    init{
        addView(wallpaper,LayoutParams(-1,-1))
        addView(View(context).apply{setBackgroundColor(0x3503070D)},LayoutParams(-1,-1))
        addView(ScrollView(context).apply{isFillViewport=true;addView(body)},LayoutParams(-1,-1))
        render()
    }

    private fun render(){
        body.removeAllViews()
        body.addView(TextView(context).apply{text="A E T H E R";textSize=10f;letterSpacing=.34f;setTextColor(0xCFFFFFFF.toInt());gravity=Gravity.CENTER},lp(-1,34))
        progress()
        when(page){0->welcome();1->homeChoice();2->systemLayer();3->finishPage()}
        val action=glassButton(if(page==3)"Enter Aether" else "Continue"){
            if(page==3){store.save(s.copy(setupComplete=true));onFinished()}else{page++;render()}
        }
        body.addView(View(context),lp(-1,14));body.addView(action,lp(-1,58))
        if(page>0)body.addView(TextView(context).apply{text="‹  Back";textSize=14f;setTextColor(0xCFFFFFFF.toInt());gravity=Gravity.CENTER;setOnClickListener{page--;render()}},lp(-1,50))
    }

    private fun progress(){
        val row=LinearLayout(context).apply{gravity=Gravity.CENTER}
        for(i in 0..3){val v=View(context).apply{setBackgroundColor(if(i==page)0xEFFFFFFF.toInt() else 0x55FFFFFF);alpha=if(i==page)1f else .8f};row.addView(v,LinearLayout.LayoutParams(dp(if(i==page)46 else 18),dp(3)).also{it.setMargins(dp(3),0,dp(3),0)})}
        body.addView(row,lp(-1,20))
    }

    private fun welcome(){
        title("Meet your new\nhome surface",42f,120)
        paragraph("Aether is not a skin over Android. It is a calm, fluid workspace built around your wallpaper, your apps and the way you move between them.")
        feature("LIQUID GLASS","Layered translucency, highlights, depth and soft atmospheric blur.")
        feature("DIRECT MANIPULATION","Long-press an app, drag it, drop it on another app to merge them.")
        feature("EDGE ACTIONS","Quick Space stays at the edge instead of stealing your home-screen gestures.")
    }

    private fun homeChoice(){
        title("How should Home behave?",34f,86)
        paragraph("This is the only setup choice that changes your first interaction. Everything remains editable later.")
        option("All apps on Home","Every installed launcher app is available directly on paged Home.",HomeMode.ALL_APPS)
        option("Home + App Drawer","Keep Home clean and swipe up for the complete library.",HomeMode.HOME_AND_DRAWER)
        option("App Drawer only","Minimal Home with the full library as the primary surface.",HomeMode.DRAWER_ONLY)
    }

    private fun systemLayer(){
        title("Unlock the Aether layer",34f,86)
        paragraph("Nothing is hidden behind a setup wall. These are optional Android capabilities; enable the ones you actually want.")
        permission("Default Home","Let Aether become the real Android Home role."){runCatching{context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))}}
        permission("Liquid Island","Allow Aether to render its floating system layer."){runCatching{context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}")))}}
        permission("Live Activities","Connect Android notifications to the Island and Activity surface."){runCatching{context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}}
        permission("App Lock","Enable foreground protection and biometric authentication."){runCatching{context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}
    }

    private fun finishPage(){
        title("You're ready.",42f,90)
        paragraph("Aether will keep your layout, folders, dock and security choices on-device. You can change the visual system at any time from Aether Settings.")
        glassPanel("YOUR STARTING SETUP","${s.homeMode.name.replace('_',' ')}  •  ${AetherRuntime.registry.launcher.apps().size} apps discovered  •  Liquid Glass on")
        glassPanel("GESTURES","Swipe horizontally for pages  •  swipe down for Search when All Apps is selected  •  pull the edge for Quick Space")
        glassPanel("MANIPULATION","Long-press + drag to reposition  •  drop app on app to merge  •  long-press dock apps to move them into Home")
    }

    private fun title(text:String,size:Float,height:Int){body.addView(TextView(context).apply{this.text=text;textSize=size;typeface=Typeface.create("sans",Typeface.BOLD);setTextColor(Color.WHITE);gravity=Gravity.CENTER;setShadowLayer(18f,0f,4f,0x70000000)},lp(-1,height))}
    private fun paragraph(text:String){body.addView(TextView(context).apply{this.text=text;textSize=15f;setTextColor(0xE8FFFFFF.toInt());gravity=Gravity.CENTER;setLineSpacing(5f,1f);setPadding(dp(6),0,dp(6),0)},lp(-1,106))}
    private fun feature(head:String,detail:String){glassPanel(head,detail)}
    private fun glassPanel(head:String,detail:String){val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(15),dp(18),dp(15));setBackground(glassDrawable(0x6F111820.toInt(),26f));elevation=dp(5).toFloat()};box.addView(TextView(context).apply{text=head;textSize=12f;letterSpacing=.12f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xF2FFFFFF.toInt())});box.addView(TextView(context).apply{text=detail;textSize=12f;setTextColor(0xCFFFFFFF.toInt());setPadding(0,dp(6),0,0);setLineSpacing(3f,1f)});body.addView(box,lp(-1,-2).also{it.setMargins(0,dp(5),0,dp(5))})}
    private fun option(head:String,detail:String,value:HomeMode){val selected=s.homeMode==value;val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(14),dp(18),dp(14));setBackground(glassDrawable(if(selected)0x9A5D8FE4.toInt()else 0x64111820.toInt(),26f));setOnClickListener{s=s.copy(homeMode=value);store.save(s);render()}};box.addView(TextView(context).apply{text=(if(selected)"●  " else "○  ")+head;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});box.addView(TextView(context).apply{text=detail;textSize=11f;setTextColor(0xCFFFFFFF.toInt());setPadding(dp(25),dp(5),0,0)});body.addView(box,lp(-1,76).also{it.setMargins(0,dp(5),0,dp(5))})}
    private fun permission(head:String,detail:String="Optional Android capability",click:()->Unit){val box=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(17),dp(11),dp(10),dp(11));setBackground(glassDrawable(0x68111820.toInt(),24f));setOnClickListener{click()}};box.addView(LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;addView(TextView(context).apply{text=head;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});addView(TextView(context).apply{text=detail;textSize=10f;setTextColor(0xBFFFFFFF.toInt());setPadding(0,dp(4),0,0)})},LinearLayout.LayoutParams(0,dp(58),1f));box.addView(TextView(context).apply{text="OPEN";textSize=10f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xEFFFFFFF.toInt());gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(58),dp(40)));body.addView(box,lp(-1,70).also{it.setMargins(0,dp(5),0,dp(5))})}
    private fun glassButton(text:String,onClick:()->Unit)=TextView(context).apply{this.text=text;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setBackground(glassDrawable(0xB65B8FEA.toInt(),28f));elevation=dp(8).toFloat();setOnClickListener{onClick()}}
    private fun glassDrawable(base:Int,radius:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(base);cornerRadius=dp(radius.toInt()).toFloat();setStroke(dp(1),0x55FFFFFF)}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h)
    private fun dp(v:Int)=(v*d).toInt()
}
