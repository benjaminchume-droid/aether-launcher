package com.aether.launcher.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.aether.launcher.system.AetherSystemActions

class QuickSpaceActivity : Activity() {
    private val d get() = resources.displayMetrics.density
    private fun dp(v:Int)=(v*d).toInt()
    private fun glass(bg:Int=0x99215A85.toInt())=GradientDrawable().apply{setColor(bg);cornerRadius=dp(26).toFloat();setStroke(dp(1),0x88FFFFFF.toInt())}
    private fun text(t:String,size:Float,bold:Boolean=false)=TextView(this).apply{ text=t; textSize=size; setTextColor(Color.WHITE); if(bold) typeface=Typeface.DEFAULT_BOLD }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(36),dp(20),dp(20));setBackgroundColor(0x6600132D)}
        val scroll=ScrollView(this); val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val header=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(20))}
        header.addView(text("∞",58f,true));header.addView(text("Quick Space",29f,true));header.addView(text("Your apps, notes, voice, and recent activity",14f))
        content.addView(header)
        val apps=panel("▦   Quick Apps","Choose your favorite shortcuts")
        val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
        val quick=listOf("WhatsApp","YouTube","Chrome","Gmail","Spotify","Camera")
        quick.forEach{ name-> row.addView(text(name,11f,true).apply{gravity=Gravity.CENTER;setPadding(dp(4),dp(18),dp(4),dp(18));setOnClickListener{launchNamed(name)}},LinearLayout.LayoutParams(0,dp(68),1f)) }
        apps.addView(row)
        apps.addView(actionButton("+   More Apps"){ startActivity(Intent(this,AetherSurfaceActivity::class.java).putExtra("surface",AetherSurface.DRAWER)) })
        content.addView(apps)
        val recent=panel("◷   Recent History","Apps you recently opened")
        recent.addView(text("Your recent app activity appears here as you use Aether.",14f).apply{setPadding(dp(8),dp(12),dp(8),dp(12))})
        recent.addView(actionButton("View all recent apps"){startActivity(Intent(this,AetherSurfaceActivity::class.java).putExtra("surface",AetherSurface.HISTORY))})
        content.addView(recent)
        val notes=panel("▤   Your Notes","Create and continue writing")
        notes.addView(actionButton("＋   New note"){startActivity(Intent(this,NoteEditorActivity::class.java))})
        notes.addView(actionButton("View all notes"){startActivity(Intent(this,NoteEditorActivity::class.java).putExtra("show_list",true))})
        content.addView(notes)
        val voice=panel("🎙   Voice Memos","Record audio on this device")
        voice.addView(actionButton("●   Start recording"){startActivity(Intent(this,VoiceRecorderActivity::class.java))})
        content.addView(voice)
        scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val footer=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        footer.addView(actionButton("←   Back"){finish()},LinearLayout.LayoutParams(0,dp(54),1f))
        footer.addView(actionButton("▦   Apps"){startActivity(Intent(this,AetherSurfaceActivity::class.java).putExtra("surface",AetherSurface.DRAWER))},LinearLayout.LayoutParams(0,dp(54),1f))
        root.addView(footer);setContentView(root)
    }
    private fun panel(title:String,subtitle:String):LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(14),dp(16),dp(14));background=glass();addView(text(title,17f,true));addView(text(subtitle,12f).apply{alpha=.8f;setPadding(0,dp(4),0,dp(8))});(layoutParams as? LinearLayout.LayoutParams)?.bottomMargin=dp(12)}
    private fun actionButton(label:String,onClick:()->Unit)=TextView(this).apply{text=label;textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=glass(0x662C8CC6);setPadding(dp(10),dp(12),dp(10),dp(12));setOnClickListener{onClick()};layoutParams=LinearLayout.LayoutParams(-1,dp(48)).apply{topMargin=dp(5);bottomMargin=dp(5)}}
    private fun launchNamed(name:String){val pm=packageManager;val intent=when(name){"WhatsApp"->pm.getLaunchIntentForPackage("com.whatsapp");"YouTube"->pm.getLaunchIntentForPackage("com.google.android.youtube");"Chrome"->pm.getLaunchIntentForPackage("com.android.chrome") ?: pm.getLaunchIntentForPackage("com.google.android.chrome");"Gmail"->pm.getLaunchIntentForPackage("com.google.android.gm");"Spotify"->pm.getLaunchIntentForPackage("com.spotify.music");"Camera"->Intent("android.media.action.IMAGE_CAPTURE");else->null};if(intent!=null)runCatching{startActivity(intent)}else startActivity(Intent(this,AetherSurfaceActivity::class.java).putExtra("surface",AetherSurface.DRAWER))}
}
