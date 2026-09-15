package com.aether.launcher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class AetherSetupView(context: Context, private val onFinished: () -> Unit) : FrameLayout(context) {
    private val store=AetherSettingsStore(context)
    private var s=store.load()
    private var page=0
    private val body=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(24),dp(32),dp(24),dp(24))}
    private val bg=ImageView(context).apply{scaleType=ImageView.ScaleType.CENTER_CROP;alpha=.72f;setImageDrawable(runCatching{WallpaperManager.getInstance(context).drawable}.getOrElse{ColorDrawable(0xFF10141A.toInt())});if(android.os.Build.VERSION.SDK_INT>=31)setRenderEffect(RenderEffect.createBlurEffect(18f,18f,Shader.TileMode.CLAMP))}

    init{addView(bg,LayoutParams(-1,-1));addView(View(context).apply{setBackgroundColor(0x26050A12)},LayoutParams(-1,-1));addView(ScrollView(context).apply{isFillViewport=true;addView(body)},LayoutParams(-1,-1));render()}

    private fun render(){body.removeAllViews();body.addView(TextView(context).apply{text="AETHER  •  ${page+1}/3";textSize=10f;letterSpacing=.28f;setTextColor(0xAFFFFFFF.toInt());gravity=Gravity.CENTER},lp(-1,32));when(page){0->welcome();1->personalize();2->ready()};body.addView(View(context),lp(-1,14));body.addView(actionButton(if(page==2)"Enter Aether" else "Continue"){if(page==2){store.save(s.copy(setupComplete=true));onFinished()}else{page++;render()}});if(page>0)body.addView(TextView(context).apply{text="‹  Back";textSize=15f;setTextColor(0xDFFFFFFF.toInt());gravity=Gravity.CENTER;setOnClickListener{page--;render()}},lp(-1,50))}

    private fun welcome(){
        title("Welcome to Aether",42f,112)
        paragraph("A wallpaper-first launcher with dimensional glass, fluid motion and a home screen you can actually arrange.")
        card("LIQUID GLASS","Wallpaper stays visible underneath surfaces. Highlights, depth and soft blur replace flat black panels.")
        card("FLUID HOME","Long-press an app, drag it anywhere, or drop it onto another app to create a folder.")
        card("SYSTEM LAYER","Dynamic Island, Quick Space, notification activities and security live beside the launcher instead of fighting it.")
    }

    private fun personalize(){
        title("Make it yours",38f,76)
        paragraph("Choose the starting home behavior. You can change everything later in Aether Settings.")
        choice("All apps on Home","Every installed launcher app is placed on the home grid.",HomeMode.ALL_APPS)
        choice("Home + Drawer","Keep a curated home and swipe up for the complete app drawer.",HomeMode.HOME_AND_DRAWER)
        choice("Drawer only","Minimal home with the drawer as the main app surface.",HomeMode.DRAWER_ONLY)
        card("APP LOCK","App Lock can be configured after setup from Aether Settings. Protected apps use Android's biometric authentication path.")
    }

    private fun ready(){
        title("Aether is ready",38f,82)
        paragraph("The launcher is designed to look good before you touch a setting. These optional system permissions unlock the deeper layer.")
        systemCard("Set as Home","Choose Aether as your Android Home app."){open(Settings.ACTION_HOME_SETTINGS)}
        systemCard("Liquid Overlay","Allow Aether to draw the Island and edge surfaces above other apps."){runCatching{context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}")))}}
        systemCard("Live Activities","Grant Notification Access for live notification and media surfaces."){runCatching{context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))}}
        systemCard("Security","Enable Aether's system integration if you want foreground app protection and relock behavior."){runCatching{context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}}
    }

    private fun title(text:String,size:Float,height:Int){body.addView(TextView(context).apply{this.text=text;textSize=size;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER},lp(-1,height))}
    private fun paragraph(text:String){body.addView(TextView(context).apply{this.text=text;textSize=15f;setTextColor(0xE8FFFFFF.toInt());gravity=Gravity.CENTER;setLineSpacing(5f,1f)},lp(-1,96))}
    private fun card(head:String,detail:String){val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(14),dp(18),dp(14));setBackgroundColor(0x38FFFFFF)};box.addView(TextView(context).apply{text=head;textSize=14f;letterSpacing=.08f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});box.addView(TextView(context).apply{text=detail;textSize=12f;setTextColor(0xCFFFFFFF.toInt());setPadding(0,dp(5),0,0)});body.addView(box,lp(-1,-2).also{it.setMargins(0,dp(5),0,dp(5))})}
    private fun choice(head:String,detail:String,value:HomeMode){val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackgroundColor(0x3CFFFFFF);setOnClickListener{s=s.copy(homeMode=value);store.save(s);render()}};box.addView(TextView(context).apply{text=(if(s.homeMode==value)"●  " else "○  ")+head;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});box.addView(TextView(context).apply{text=detail;textSize=11f;setTextColor(0xBFFFFFFF.toInt());setPadding(dp(24),dp(4),0,0)});body.addView(box,lp(-1,70).also{it.setMargins(0,dp(4),0,dp(4))})}
    private fun systemCard(head:String,detail:String,onClick:()->Unit){val box=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(14),dp(18),dp(14));setBackgroundColor(0x3CFFFFFF);setOnClickListener{onClick()}};box.addView(TextView(context).apply{text=head;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});box.addView(TextView(context).apply{text=detail;textSize=11f;setTextColor(0xCFFFFFFF.toInt());setPadding(0,dp(4),0,0)});body.addView(box,lp(-1,-2).also{it.setMargins(0,dp(5),0,dp(5))})}
    private fun actionButton(text:String,onClick:()->Unit)=Button(context).apply{this.text=text;textSize=16f;setTextColor(Color.WHITE);setAllCaps(false);setBackgroundColor(0xB83B76D4.toInt());setOnClickListener{onClick()}}
    private fun open(action:String){runCatching{context.startActivity(Intent(action))}}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
