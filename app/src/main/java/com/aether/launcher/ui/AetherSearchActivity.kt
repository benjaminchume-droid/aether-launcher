package com.aether.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.launcher.AppInfo

class AetherSearchActivity:Activity(){
 private val allApps by lazy{AetherRuntime.registry.launcher.apps()};private val history by lazy{AetherHistoryStore(this)};private lateinit var results:LinearLayout;private lateinit var empty:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b);AetherRuntime.initialize(applicationContext);window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(28),dp(20),dp(20));setBackgroundColor(0xFF080A0E.toInt())};root.addView(TextView(this).apply{text="Search Aether";textSize=30f;setTextColor(Color.WHITE);typeface=android.graphics.Typeface.DEFAULT_BOLD},LinearLayout.LayoutParams(-1,dp(48)));val query=EditText(this).apply{hint="Search installed apps…";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(0x88FFFFFF.toInt());setBackgroundColor(0x221FFFFFFF.toInt());setPadding(dp(16),0,dp(16),0)};root.addView(query,LinearLayout.LayoutParams(-1,dp(56)).apply{bottomMargin=dp(14)});val scroll=ScrollView(this);results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(results);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));empty=TextView(this).apply{text="Search installed apps";textSize=14f;setTextColor(0xBFFFFFFF.toInt());gravity=Gravity.CENTER};root.addView(empty,LinearLayout.LayoutParams(-1,dp(70)));setContentView(root);query.requestFocus();query.post{(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(query,InputMethodManager.SHOW_IMPLICIT)};query.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){render(s?.toString().orEmpty())};override fun afterTextChanged(s:android.text.Editable?){}});render("")}
 private fun render(raw:String){results.removeAllViews();val q=raw.trim().lowercase();val list=if(q.isBlank())history.load().mapNotNull{h->allApps.firstOrNull{it.packageName==h.packageName}}.distinctBy{it.packageName}.let{it+allApps.filterNot{a->it.any{r->r.packageName==a.packageName}}.take(12-it.size.coerceAtMost(12)).toList()}.take(12)else allApps.asSequence().filter{it.label.lowercase().contains(q)||it.packageName.lowercase().contains(q)}.take(30).toList();list.forEach(::addResult);empty.visibility=if(list.isEmpty())TextView.VISIBLE else TextView.GONE;if(q.isNotBlank()&&list.isEmpty())empty.text="No installed app matches \"$raw\"";else empty.text="Recent apps • installed apps"}
 private fun addResult(app:AppInfo){val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,dp(12),0);setBackgroundColor(0x141FFFFFFF.toInt());setOnClickListener{history.record(app.packageName);AetherRuntime.registry.launcher.launchIntent(app.packageName)?.let(::startActivity);finish()}};row.addView(ImageView(this).apply{setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName));setPadding(dp(5),dp(5),dp(5),dp(5))},LinearLayout.LayoutParams(dp(54),dp(62)));val labels=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL};labels.addView(TextView(this).apply{text=app.label;textSize=15f;setTextColor(Color.WHITE)});labels.addView(TextView(this).apply{text=app.packageName;textSize=10f;setTextColor(0x7FFFFFFF.toInt())});row.addView(labels,LinearLayout.LayoutParams(0,dp(62),1f));results.addView(row,LinearLayout.LayoutParams(-1,dp(66)).apply{bottomMargin=dp(6)})}
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
}
