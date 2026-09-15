package com.aether.launcher.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.aether.launcher.AetherRuntime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AetherToolsActivity : Activity() {
    companion object { const val EXTRA_TOOL = "tool"; private const val REQ_MIC = 100; private const val REQ_CAPTURE = 101 }
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    override fun onCreate(state: Bundle?) { super.onCreate(state); when (intent.getStringExtra(EXTRA_TOOL)) { "notes" -> note(); "calculator" -> calculator(); "clipboard" -> clipboard(); "voice" -> voice(); "screenshot" -> screenshotConsent(); else -> finish() } }
    private fun note() { val input=EditText(this).apply{hint="Write a note"}; android.app.AlertDialog.Builder(this).setTitle("Aether Note").setView(input).setNegativeButton("Cancel"){_,_->finish()}.setPositiveButton("Save"){_,_->input.text.toString().takeIf{it.isNotBlank()}?.let{AetherRuntime.registry.notes.capture(it)};finish()}.show() }
    private fun calculator(){val input=EditText(this).apply{hint="12 * 8 + 4";inputType=2 or 8192};android.app.AlertDialog.Builder(this).setTitle("Calculator").setView(input).setNegativeButton("Close"){_,_->finish()}.setPositiveButton("Calculate"){_,_->android.widget.Toast.makeText(this,evaluate(input.text.toString())?:"Invalid expression",android.widget.Toast.LENGTH_LONG).show()}.show()}
    private fun clipboard(){val cm=getSystemService(android.content.ClipboardManager::class.java);val text=cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty();setContentView(TextView(this).apply{setText(if(text.isBlank())"Clipboard is empty" else text);textSize=18f;setPadding(28,40,28,40)})}
    private fun voice(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,48,32,48)};box.addView(TextView(this).apply{text="Voice Recorder";textSize=28f});val start=Button(this).apply{text="Start recording"};val stop=Button(this).apply{text="Stop & save";isEnabled=false};box.addView(start);box.addView(stop);setContentView(box);start.setOnClickListener{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_MIC);return@setOnClickListener};recordingFile=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"aether_${stamp()}.m4a");recorder=MediaRecorder(this).apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setOutputFile(recordingFile!!.absolutePath);prepare();start()};start.isEnabled=false;stop.isEnabled=true};stop.setOnClickListener{runCatching{recorder?.stop()};runCatching{recorder?.release()};recorder=null;android.widget.Toast.makeText(this,"Saved ${recordingFile?.name}",android.widget.Toast.LENGTH_LONG).show();finish()}}
    private fun screenshotConsent(){val mgr=getSystemService(MediaProjectionManager::class.java);startActivityForResult(mgr.createScreenCaptureIntent(),REQ_CAPTURE)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_CAPTURE&&resultCode==RESULT_OK){android.widget.Toast.makeText(this,"Screen capture permission granted",android.widget.Toast.LENGTH_LONG).show()};finish()}
    private fun stamp()=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())
    private fun evaluate(s:String):String?=runCatching{val c=s.replace(" ","");if(!c.matches(Regex("[-+*/.0-9]+")))return null;val t=c.split(Regex("(?=[-+*/])|(?<=[-+*/])")).filter{it.isNotEmpty()};var r=t[0].toDouble();var i=1;while(i+1<t.size){val n=t[i+1].toDouble();r=when(t[i]){"+"->r+n;"-"->r-n;"*"->r*n;"/"->r/n;else->return null};i+=2};if(r%1==0.0)r.toLong().toString() else "%.4f".format(r)}.getOrNull()
}
