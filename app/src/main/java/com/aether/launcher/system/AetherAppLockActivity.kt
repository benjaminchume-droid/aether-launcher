package com.aether.launcher.system

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import com.aether.launcher.AetherRuntime

/** Visual app-lock policy editor. Authentication itself is handled by LockActivity. */
class AetherAppLockActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        AetherRuntime.initialize(applicationContext)
        val density=resources.displayMetrics.density
        fun dp(v:Int)=(v*density).toInt()
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(30),dp(20),dp(20));setBackgroundColor(0xFF0A0D12.toInt())}
        root.addView(TextView(this).apply{text="App Lock";textSize=34f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(-1,dp(50)))
        root.addView(TextView(this).apply{text="Choose the apps Aether should protect.";textSize=13f;setTextColor(0xBFFFFFFF.toInt())},LinearLayout.LayoutParams(-1,dp(42)))
        val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        AetherRuntime.registry.launcher.apps().forEach { app ->
            val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,dp(8),0);setBackgroundColor(0x28FFFFFF.toInt())}
            row.addView(ImageView(this).apply{setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName))},LinearLayout.LayoutParams(dp(48),dp(58)))
            row.addView(TextView(this).apply{text=app.label;textSize=15f;setTextColor(Color.WHITE);setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(58),1f))
            row.addView(Switch(this).apply{isChecked=AetherRuntime.security.isProtected(app.packageName);setOnCheckedChangeListener{_,checked->if(checked)AetherRuntime.security.protect(app.packageName)else AetherRuntime.security.unprotect(app.packageName)}},LinearLayout.LayoutParams(dp(58),dp(58)))
            list.addView(row,LinearLayout.LayoutParams(-1,dp(62)).apply{bottomMargin=dp(5)})
        }
        scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
    }
}
