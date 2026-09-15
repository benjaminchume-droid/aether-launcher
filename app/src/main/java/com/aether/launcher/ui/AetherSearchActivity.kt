package com.aether.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import androidx.core.view.WindowCompat
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.launcher.AppInfo

class AetherSearchActivity : Activity() {
    private val apps by lazy { AetherRuntime.registry.launcher.apps() }
    private val history by lazy { AetherHistoryStore(this) }
    private lateinit var results: LinearLayout
    private lateinit var empty: TextView
    private val d get()=resources.displayMetrics.density

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        AetherRuntime.initialize(applicationContext)
        val root=AetherGlassRoot(this)
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(42),dp(24),dp(20))}
        body.addView(TextView(this).apply{text="SEARCH AETHER";textSize=10f;letterSpacing=.28f;setTextColor(0xBFFFFFFF.toInt())},LinearLayout.LayoutParams(-1,dp(28)))
        body.addView(TextView(this).apply{text="Find your apps.";textSize=36f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(-1,dp(52)))
        body.addView(TextView(this).apply{text="Search by app name • no package IDs • instant results";textSize=12f;setTextColor(0xCFFFFFFF.toInt())},LinearLayout.LayoutParams(-1,dp(28)))
        val query=EditText(this).apply{hint="Search apps…";setSingleLine();textSize=18f;setTextColor(Color.WHITE);setHintTextColor(0x75FFFFFF);setPadding(dp(18),0,dp(18),0);background=glass(0x82111820,22f)}
        body.addView(query,LinearLayout.LayoutParams(-1,dp(62)).apply{topMargin=dp(16);bottomMargin=dp(14)})
        val scroll=ScrollView(this).apply{isFillViewport=true};results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(results);body.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        empty=TextView(this).apply{textSize=14f;setTextColor(0xBFFFFFFF.toInt());gravity=Gravity.CENTER;text="Recent apps"}
        body.addView(empty,LinearLayout.LayoutParams(-1,dp(54)))
        root.addView(body,android.view.ViewGroup.LayoutParams(-1,-1));setContentView(root)
        query.requestFocus();query.post{(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(query,InputMethodManager.SHOW_IMPLICIT)}
        query.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){render(s?.toString().orEmpty())};override fun afterTextChanged(s:android.text.Editable?) {}})
        render("")
    }

    private fun render(raw:String){
        results.removeAllViews();val q=raw.trim().lowercase()
        val recent=history.load().mapNotNull{h->apps.firstOrNull{it.packageName==h.packageName}}.distinctBy{it.packageName}
        val list=if(q.isBlank())(recent+apps.filterNot{a->recent.any{it.packageName==a.packageName}}).take(60)else apps.filter{it.label.lowercase().contains(q)}.take(60)
        list.forEach(::addResult);empty.visibility=if(list.isEmpty())View.VISIBLE else View.GONE
        empty.text=if(list.isEmpty())"No app found for \"$raw\"" else if(q.isBlank())"${apps.size} installed apps • recent first" else "${list.size} matches"
    }

    private fun addResult(app:AppInfo){
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(6),dp(14),dp(6));background=glass(0x6F111820,20f);setOnClickListener{history.record(app.packageName);AetherRuntime.registry.launcher.launchIntent(app.packageName)?.let(::startActivity);finish()}}
        row.addView(ImageView(this).apply{setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName));setPadding(dp(2),dp(2),dp(2),dp(2))},LinearLayout.LayoutParams(dp(58),dp(58)))
        row.addView(TextView(this).apply{text=app.label;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(58),1f))
        row.addView(TextView(this).apply{text="›";textSize=22f;setTextColor(0x80FFFFFF.toInt());gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(58)))
        results.addView(row,LinearLayout.LayoutParams(-1,dp(72)).apply{bottomMargin=dp(8)})
    }

    private fun glass(base:Int,r:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(base);cornerRadius=dp(r.toInt()).toFloat();setStroke(dp(1),0x45FFFFFF)}
    private fun dp(v:Int)=(v*d).toInt()
}
