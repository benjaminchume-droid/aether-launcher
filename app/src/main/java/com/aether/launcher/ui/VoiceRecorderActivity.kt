package com.aether.launcher.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceRecorderActivity:Activity(){
 private var recorder:MediaRecorder?=null
 private var output:File?=null
 private var recording=false
 private var paused=false
 private lateinit var status:TextView
 private lateinit var timer:TextView
 private lateinit var record:TextView
 private lateinit var pause:TextView
 private var startedAt=0L
 private val handler=android.os.Handler(android.os.Looper.getMainLooper())
 private val tick=object:Runnable{override fun run(){if(recording&&!paused){timer.text=String.format(Locale.getDefault(),"%02d:%02d",(SystemClock.elapsedRealtime()-startedAt)/60000,((SystemClock.elapsedRealtime()-startedAt)/1000)%60);handler.postDelayed(this,250)}}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=Color.TRANSPARENT;window.navigationBarColor=Color.TRANSPARENT
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(dp(24),dp(30),dp(24),dp(24));setBackgroundColor(0xFF061D35.toInt())}
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  top.addView(TextView(this).apply{text="Voice Memo";textSize=24f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,dp(60),1f))
  top.addView(TextView(this).apply{text="✕";textSize=26f;setTextColor(Color.WHITE);setOnClickListener{if(recording)stopAndSave(true);finish()}},LinearLayout.LayoutParams(dp(48),dp(60)))
  root.addView(top)
  status=TextView(this).apply{text="Ready to record";textSize=16f;setTextColor(0xCCFFFFFF.toInt());gravity=Gravity.CENTER}
  timer=TextView(this).apply{text="00:00";textSize=48f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER}
  root.addView(status,LinearLayout.LayoutParams(-1,dp(70)));root.addView(timer,LinearLayout.LayoutParams(-1,dp(110)))
  root.addView(TextView(this).apply{text="Recordings are stored locally on your device.";textSize=13f;setTextColor(0xAAFFFFFF.toInt());gravity=Gravity.CENTER})
  val spacer=TextView(this).apply{text=""};root.addView(spacer,LinearLayout.LayoutParams(1,0,1f))
  val controls=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER}
  pause=button("Ⅱ",0x664F7FA7);pause.isEnabled=false;pause.alpha=.45f;pause.setOnClickListener{togglePause()}
  record=button("●",0xFF167BE8.toInt());record.setOnClickListener{if(!recording)startRecording()else stopAndSave(false)}
  controls.addView(pause,LinearLayout.LayoutParams(dp(72),dp(72)).apply{rightMargin=dp(28)})
  controls.addView(record,LinearLayout.LayoutParams(dp(92),dp(92)))
  root.addView(controls);root.addView(TextView(this).apply{text="Tap the circle to start recording";textSize=13f;setTextColor(0xAAFFFFFF.toInt());gravity=Gravity.CENTER;setPadding(0,dp(18),0,dp(20))})
  setContentView(root)
 }
 private fun button(t:String,color:Int)=TextView(this).apply{text=t;textSize=30f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(color);setStroke(dp(2),0x99FFFFFF.toInt())}}
 private fun startRecording(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),22);return}
  val dir=File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),"Aether Voice Memos").apply{mkdirs()}
  output=File(dir,"voice_${SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())}.m4a")
  try{recorder=if(Build.VERSION.SDK_INT>=31)MediaRecorder(this)else @Suppress("DEPRECATION") MediaRecorder()
   recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC);recorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder!!.setAudioEncodingBitRate(128000);recorder!!.setAudioSamplingRate(44100);recorder!!.setOutputFile(output!!.absolutePath);recorder!!.prepare();recorder!!.start()
   recording=true;paused=false;startedAt=SystemClock.elapsedRealtime();status.text="Recording";record.text="■";record.contentDescription="End recording";pause.isEnabled=true;pause.alpha=1f;handler.post(tick)
  }catch(e:Exception){recorder?.release();recorder=null;output?.delete();recording=false;Toast.makeText(this,"Could not start recording: ${e.localizedMessage}",Toast.LENGTH_LONG).show()}
 }
 private fun togglePause(){if(!recording)return
  if(Build.VERSION.SDK_INT>=24){try{if(!paused){recorder?.pause();paused=true;status.text="Paused";pause.text="▶";handler.removeCallbacks(tick)}else{recorder?.resume();paused=false;status.text="Recording";pause.text="Ⅱ";handler.post(tick)}}catch(e:Exception){Toast.makeText(this,"Pause/resume unavailable on this device",Toast.LENGTH_SHORT).show()}}
  else Toast.makeText(this,"Pause requires Android 7.0 or later",Toast.LENGTH_SHORT).show()
 }
 private fun stopAndSave(discard:Boolean){if(!recording){if(discard)output?.delete();return};handler.removeCallbacks(tick);recording=false;runCatching{recorder?.stop()};recorder?.release();recorder=null;paused=false
  if(discard){output?.delete();status.text="Recording discarded"}else{status.text="Saved to Voice Memos";Toast.makeText(this,"Recording saved",Toast.LENGTH_LONG).show()}
  record.text="●";record.contentDescription="Record";pause.isEnabled=false;pause.alpha=.45f;pause.text="Ⅱ";timer.text="00:00"
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==22&&grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startRecording()else Toast.makeText(this,"Microphone permission is required to record.",Toast.LENGTH_LONG).show()}
 override fun onDestroy(){handler.removeCallbacks(tick);if(recording)stopAndSave(false);super.onDestroy()}
}
