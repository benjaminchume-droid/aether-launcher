package com.aether.launcher.system

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import com.aether.launcher.AetherRuntime
import com.aether.launcher.ui.AetherGlassRoot

/** Visual policy editor for Aether App Lock. */
class AetherAppLockActivity : ComponentActivity() {
    private val d get()=resources.displayMetrics.density
    private lateinit var list:LinearLayout

    override fun onCreate(state:Bundle?){
        super.onCreate(state);AetherRuntime.initialize(applicationContext)
        val root=AetherGlassRoot(this);val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(38),dp(22),dp(24))}
        body.addView(TextView(this).apply{text="SECURITY  /  APP LOCK";textSize=10f;letterSpacing=.25f;setTextColor(0xAFFFFFFF.toInt())},LinearLayout.LayoutParams(-1,dp(28)))
        body.addView(TextView(this).apply{text="Protect what matters.";textSize=34f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(-1,dp(52)))
        body.addView(TextView(this).apply{text="Select an app once. Aether remembers it and requests biometric or device credential when it becomes active.";textSize=12f;setTextColor(0xCFFFFFFF.toInt());setLineSpacing(3f,1f)},LinearLayout.LayoutParams(-1,dp(62)))
        val status=TextView(this).apply{setTextColor(0xBFFFFFFF.toInt());textSize=11f;setPadding(dp(14),0,0,0);gravity=Gravity.CENTER_VERTICAL;background=glass(0x68101821,22f)}
        body.addView(status,LinearLayout.LayoutParams(-1,dp(48)).also{it.bottomMargin=dp(10)})
        list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val scroll=ScrollView(this).apply{isFillViewport=true;addView(list)};body.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));root.addView(body,android.view.ViewGroup.LayoutParams(-1,-1));setContentView(root);render(status)
    }

    private fun render(status:TextView){
        list.removeAllViews();val apps=AetherRuntime.registry.launcher.apps();val locked=apps.count{AetherRuntime.security.isProtected(it.packageName)}
        status.text=if(locked==0)"No apps protected yet" else "$locked protected  •  biometric / device credential"
        apps.forEach{app->
            val protected=AetherRuntime.security.isProtected(app.packageName)
            val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(7),dp(8),dp(7));background=glass(if(protected)0x824B6E9A else 0x5A101821,22f)}
            row.addView(ImageView(this).apply{setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName));setPadding(dp(2),dp(2),dp(2),dp(2))},LinearLayout.LayoutParams(dp(54),dp(54)))
            row.addView(LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;addView(TextView(this@AetherAppLockActivity).apply{text=app.label;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)});addView(TextView(this@AetherAppLockActivity).apply{text=if(protected)"Protected" else "Tap to protect";textSize=10f;setTextColor(if(protected)0xD0FFFFFF.toInt() else 0x8FFFFFFF.toInt());setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,dp(54),1f))
            row.addView(Switch(this).apply{isChecked=protected;setOnCheckedChangeListener{_,checked->if(checked)AetherRuntime.security.protect(app.packageName)else AetherRuntime.security.unprotect(app.packageName);render(status)}},LinearLayout.LayoutParams(dp(58),dp(54)))
            list.addView(row,LinearLayout.LayoutParams(-1,dp(68)).also{it.bottomMargin=dp(7)})
        }
    }

    private fun glass(base:Int,r:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(base);cornerRadius=dp(r.toInt()).toFloat();setStroke(dp(1),0x42FFFFFF)}
    private fun dp(v:Int)=(v*d).toInt()
}
