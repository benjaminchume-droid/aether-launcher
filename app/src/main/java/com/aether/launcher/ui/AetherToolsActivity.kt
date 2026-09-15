package com.aether.launcher.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.aether.launcher.AetherRuntime
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AetherToolsActivity : Activity() {
    companion object { const val EXTRA_TOOL="tool"; private const val REQ_MIC=100; private const val REQ_CAPTURE=101 }
    private var recorder:MediaRecorder?=null
    private var recordingFile:File?=null
    private var projection:MediaProjection?=null
    private var reader:ImageReader?=null
    private var virtualDisplay:android.hardware.display.VirtualDisplay?=null
    override fun onCreate(state:Bundle?){super.onCreate(state);when(intent.getStringExtra(EXTRA_TOOL)){"notes"->note();"calculator"->calculator();"clipboard"->clipboard();"voice"->voice();"screenshot"->screenshotConsent();else->finish()}}
    private fun note(){val input=EditText(this).apply{hint="Write a note"};android.app.AlertDialog.Builder(this).setTitle("Aether Note").setView(input).setNegativeButton("Cancel"){_,_->finish()}.setPositiveButton("Save"){_,_->input.text.toString().takeIf{it.isNotBlank()}?.let{AetherRuntime.registry.notes.capture(it)};finish()}.show()}
    private fun calculator(){val input=EditText(this).apply{hint="12 * 8 + 4";inputType=2 or 8192};android.app.AlertDialog.Builder(this).setTitle("Calculator").setView(input).setNegativeButton("Close"){_,_->finish()}.setPositiveButton("Calculate"){_,_->Toast.makeText(this,evaluate(input.text.toString())?:"Invalid expression",Toast.LENGTH_LONG).show()}.show()}
    private fun clipboard(){val cm=getSystemService(android.content.ClipboardManager::class.java);val text=cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty();setContentView(TextView(this).apply{textSize=18f;setPadding(28,40,28,40);text=if(text.isBlank())"Clipboard is empty" else text})}
    private fun voice(){val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,48,32,48)};box.addView(TextView(this).apply{text="Voice Recorder";textSize=28f});val start=Button(this).apply{text="Start recording"};val stop=Button(this).apply{text="Stop & save";isEnabled=false};box.addView(start);box.addView(stop);setContentView(box);start.setOnClickListener{if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_MIC);return@setOnClickListener};recordingFile=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"aether_${stamp()}.m4a");recorder=MediaRecorder(this).apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setOutputFile(recordingFile!!.absolutePath);prepare();start()};start.isEnabled=false;stop.isEnabled=true};stop.setOnClickListener{runCatching{recorder?.stop()};runCatching{recorder?.release()};recorder=null;Toast.makeText(this,"Saved ${recordingFile?.name}",Toast.LENGTH_LONG).show();finish()}}
    private fun screenshotConsent(){startActivityForResult(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent(),REQ_CAPTURE)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=REQ_CAPTURE){return};if(resultCode!=RESULT_OK||data==null){finish();return};runCatching{startCapture(data)}.onFailure{Toast.makeText(this,"Screenshot failed: ${it.message}",Toast.LENGTH_LONG).show();finish()}}
    private fun startCapture(data:Intent){val dm=resources.displayMetrics;val width=dm.widthPixels.coerceAtLeast(1);val height=dm.heightPixels.coerceAtLeast(1);projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(RESULT_OK,data);reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);reader!!.setOnImageAvailableListener({ir->val image=ir.acquireLatestImage()?:return@setOnImageAvailableListener;try{val plane=image.planes[0];val buffer:ByteBuffer=plane.buffer;val pixelStride=plane.pixelStride;val rowStride=plane.rowStride;val rowPadding=rowStride-pixelStride*width;val bitmap=Bitmap.createBitmap(width+rowPadding/pixelStride,height,Bitmap.Config.ARGB_8888);bitmap.copyPixelsFromBuffer(buffer);val cropped=if(bitmap.width!=width)Bitmap.createBitmap(bitmap,0,0,width,height)else bitmap;saveScreenshot(cropped);if(cropped!==bitmap)bitmap.recycle();cropped.recycle()}finally{image.close();stopCapture()}},Handler(Looper.getMainLooper()));virtualDisplay=projection!!.createVirtualDisplay("AetherScreenshot",width,height,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,null);Handler(Looper.getMainLooper()).postDelayed({if(reader!=null){stopCapture();Toast.makeText(this,"No frame received",Toast.LENGTH_LONG).show()}},1500)}
    private fun saveScreenshot(bitmap:Bitmap){val name="Aether_${stamp()}.png";if(android.os.Build.VERSION.SDK_INT>=29){val values=android.content.ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/png");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Aether");put(MediaStore.Images.Media.IS_PENDING,1)};val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)?:throw IllegalStateException("MediaStore insert failed");contentResolver.openOutputStream(uri)?.use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}?:throw IllegalStateException("Could not write screenshot");values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);contentResolver.update(uri,values,null,null)}else{val dir=File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"Aether").apply{mkdirs()};java.io.FileOutputStream(File(dir,name)).use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}};Toast.makeText(this,"Screenshot saved",Toast.LENGTH_SHORT).show()}
    private fun stopCapture(){runCatching{virtualDisplay?.release()};virtualDisplay=null;runCatching{reader?.close()};reader=null;runCatching{projection?.stop()};projection=null;if(!isFinishing)finish()}
    override fun onDestroy(){runCatching{recorder?.release()};runCatching{virtualDisplay?.release()};runCatching{reader?.close()};runCatching{projection?.stop()};super.onDestroy()}
    private fun stamp()=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())
    private fun evaluate(s:String):String?=runCatching{val c=s.replace(" ","");if(!c.matches(Regex("[-+*/.0-9]+")))return null;val t=c.split(Regex("(?=[-+*/])|(?<=[-+*/])")).filter{it.isNotEmpty()};var r=t[0].toDouble();var i=1;while(i+1<t.size){val n=t[i+1].toDouble();r=when(t[i]){"+"->r+n;"-"->r-n;"*"->r*n;"/"->if(n==0.0)return null else r/n;else->return null};i+=2};if(r%1==0.0)r.toLong().toString() else "%.4f".format(r)}.getOrNull()
}
