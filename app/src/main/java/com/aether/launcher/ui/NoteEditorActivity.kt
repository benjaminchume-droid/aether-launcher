package com.aether.launcher.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class NoteEditorActivity:Activity(){
 private val prefs by lazy{getSharedPreferences("aether_notes_v1",MODE_PRIVATE)}
 private var noteId:String?=null
 private lateinit var title:EditText
 private lateinit var body:EditText
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT;window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
  if(intent.getBooleanExtra("show_list",false)){showNotes();return}
  noteId=intent.getStringExtra("note_id")
  val existing=readNotes().firstOrNull{it.optString("id")==noteId}
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(38),dp(20),dp(16));setBackgroundColor(0xFF071D33.toInt())}
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  top.addView(TextView(this).apply{text="‹";textSize=34f;setTextColor(Color.WHITE);setOnClickListener{save();finish()}},LinearLayout.LayoutParams(dp(48),dp(54)))
  title=EditText(this).apply{hint="Note title";setTextColor(Color.WHITE);setHintTextColor(0x99FFFFFF.toInt());textSize=23f;typeface=Typeface.DEFAULT_BOLD;setSingleLine(true);setText(existing?.optString("title")?:"")}
  top.addView(title,LinearLayout.LayoutParams(0,dp(54),1f))
  top.addView(TextView(this).apply{text="Save";textSize=15f;setTextColor(Color.WHITE);setPadding(dp(10),dp(10),dp(10),dp(10));setOnClickListener{save();finish()} })
  root.addView(top)
  root.addView(TextView(this).apply{text="Write freely. Your notes are saved on this device.";textSize=12f;setTextColor(0xBFFFFFFF.toInt());setPadding(dp(4),dp(6),0,dp(10))})
  body=EditText(this).apply{hint="Start writing…";setTextColor(Color.WHITE);setHintTextColor(0x88FFFFFF.toInt());textSize=17f;gravity=Gravity.TOP;minLines=10;setPadding(dp(4),dp(8),dp(4),dp(8));setBackgroundColor(Color.TRANSPARENT);setText(existing?.optString("body")?:"")}
  root.addView(body,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
 }
 private fun readNotes():MutableList<JSONObject>{val out=mutableListOf<JSONObject>();runCatching{val a=JSONArray(prefs.getString("items","[]"));for(i in 0 until a.length())out+=a.getJSONObject(i)};return out}
 private fun save(){if(!::title.isInitialized||!::body.isInitialized)return;val all=readNotes();val id=noteId?:java.util.UUID.randomUUID().toString();val obj=JSONObject().put("id",id).put("title",title.text.toString().ifBlank{"Untitled note"}).put("body",body.text.toString()).put("updated",System.currentTimeMillis());val index=all.indexOfFirst{it.optString("id")==id};if(index>=0)all[index]=obj else all.add(0,obj);prefs.edit().putString("items",JSONArray(all).toString()).apply();noteId=id}
 private fun showNotes(){val all=readNotes();val labels=all.map{it.optString("title","Untitled note")}.toTypedArray();AlertDialog.Builder(this).setTitle("Your Notes").setItems(labels){_,i->startActivity(android.content.Intent(this,NoteEditorActivity::class.java).putExtra("note_id",all[i].optString("id")))}.setPositiveButton("New note"){_,_->startActivity(android.content.Intent(this,NoteEditorActivity::class.java))}.setNegativeButton("Close"){_,_->finish()}.show()}
 override fun onPause(){if(::title.isInitialized&&::body.isInitialized)save();super.onPause()}
}
