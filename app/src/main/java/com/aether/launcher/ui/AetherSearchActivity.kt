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
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.launcher.AppInfo

class AetherSearchActivity : Activity() {
    private val apps by lazy { AetherRuntime.registry.launcher.apps() }
    private val history by lazy { AetherHistoryStore(this) }
    private lateinit var results: LinearLayout
    private lateinit var empty: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        AetherRuntime.initialize(applicationContext)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(34),dp(22),dp(20)); setBackgroundColor(Color.TRANSPARENT) }
        val title=TextView(this).apply { text="Find anything";textSize=34f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD }
        root.addView(title,LinearLayout.LayoutParams(-1,dp(50)))
        root.addView(TextView(this).apply{text="Swipe down from Home to open search";textSize=12f;setTextColor(0xBFFFFFFF.toInt())},LinearLayout.LayoutParams(-1,dp(28)))
        val query=EditText(this).apply{hint="Apps, not package names";setSingleLine();textSize=18f;setTextColor(Color.WHITE);setHintTextColor(0x80FFFFFF.toInt());setPadding(dp(18),0,dp(18),0);setBackgroundColor(0x3AFFFFFF)}
        root.addView(query,LinearLayout.LayoutParams(-1,dp(62)).apply{topMargin=dp(12);bottomMargin=dp(16)})
        val scroll=ScrollView(this);results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(results);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        empty=TextView(this).apply{textSize=14f;setTextColor(0xBFFFFFFF.toInt());gravity=Gravity.CENTER;text="Recent and installed apps"};root.addView(empty,LinearLayout.LayoutParams(-1,dp(54)))
        setContentView(root)
        query.requestFocus();query.post{(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(query,InputMethodManager.SHOW_IMPLICIT)}
        query.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){render(s?.toString().orEmpty())};override fun afterTextChanged(s:android.text.Editable?){}})
        render("")
    }

    private fun render(raw:String){
        results.removeAllViews();val q=raw.trim().lowercase()
        val recent=history.load().mapNotNull{h->apps.firstOrNull{it.packageName==h.packageName}}.distinctBy{it.packageName}
        val list=if(q.isBlank())(recent+apps.filterNot{a->recent.any{it.packageName==a.packageName}}).take(24)else apps.filter{it.label.lowercase().contains(q)}.take(40)
        list.forEach(::addResult);empty.visibility=if(list.isEmpty())View.VISIBLE else View.GONE
        if(list.isEmpty())empty.text="No app found for \"$raw\"" else empty.text=if(q.isBlank())"Recent first • ${apps.size} apps installed" else "${list.size} matches"
    }

    private fun addResult(app:AppInfo){
        val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(6),dp(14),dp(6));setBackgroundColor(0x2BFFFFFF);setOnClickListener{history.record(app.packageName);AetherRuntime.registry.launcher.launchIntent(app.packageName)?.let(::startActivity);finish()}}
        row.addView(ImageView(this).apply{setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName));setPadding(dp(3),dp(3),dp(3),dp(3))},LinearLayout.LayoutParams(dp(58),dp(58)))
        row.addView(TextView(this).apply{text=app.label;textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(58),1f))
        results.addView(row,LinearLayout.LayoutParams(-1,dp(68)).apply{bottomMargin=dp(8)})
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
