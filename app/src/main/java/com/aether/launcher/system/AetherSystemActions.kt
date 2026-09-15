package com.aether.launcher.system

import android.Manifest
import android.app.ActivityOptions
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

object AetherSystemActions {
    fun launchHome(context:Context){context.startActivity(Intent(context,com.aether.launcher.AetherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))}
    fun openSettings(context:Context){context.startActivity(Intent(context,com.aether.launcher.settings.AetherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
    fun openSurface(context:Context,surface:String){context.startActivity(Intent(context,com.aether.launcher.ui.AetherSurfaceActivity::class.java).putExtra("surface",surface).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
    fun launchPackage(context:Context,packageName:String):Boolean=runCatching{context.packageManager.getLaunchIntentForPackage(packageName)?.let{context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));true}?:false}.getOrDefault(false)
    fun launchAdjacent(context:Context,packageName:String):Boolean=runCatching{val intent=context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)?:return false;if(Build.VERSION.SDK_INT>=24){val dm=context.resources.displayMetrics;val bounds=Rect(0,0,dm.widthPixels/2,dm.heightPixels);val options=ActivityOptions.makeBasic().setLaunchBounds(bounds);context.startActivity(intent,options.toBundle())}else context.startActivity(intent);true}.getOrDefault(false)
    fun launchBounded(context:Context,packageName:String,side:Int):Boolean=runCatching{val intent=context.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?:return false;if(Build.VERSION.SDK_INT>=24){val dm=context.resources.displayMetrics;val half=dm.widthPixels/2;val left=if(side<0)0 else half;val bounds=Rect(left,0,if(side<0)half else dm.widthPixels,dm.heightPixels);context.startActivity(intent,ActivityOptions.makeBasic().setLaunchBounds(bounds).toBundle())}else context.startActivity(intent);true}.getOrDefault(false)
    fun toggleTorch(context:Context):Boolean{if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){(context as? android.app.Activity)?.requestPermissions(arrayOf(Manifest.permission.CAMERA),707);return false};return runCatching{val manager=context.getSystemService(CameraManager::class.java);val id=manager.cameraIdList.firstOrNull{manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true}?:return false;val prefs=context.getSharedPreferences("aether_controls",Context.MODE_PRIVATE);val enabled=prefs.getBoolean("torch",false);manager.setTorchMode(id,!enabled);prefs.edit().putBoolean("torch",!enabled).apply();true}.getOrDefault(false)}
    fun volume(context:Context,direction:Int){context.getSystemService(AudioManager::class.java).adjustVolume(direction,AudioManager.FLAG_SHOW_UI)}
    fun setBrightness(context:Context,percent:Int):Boolean{if(!Settings.System.canWrite(context)){requestWriteSettings(context);return false};return runCatching{Settings.System.putInt(context.contentResolver,Settings.System.SCREEN_BRIGHTNESS,(percent.coerceIn(1,100)*255/100).coerceIn(1,255))}.getOrDefault(false)}
    fun adjustBrightness(context:Context,delta:Int):Boolean{if(!Settings.System.canWrite(context)){requestWriteSettings(context);return false};return runCatching{val old=Settings.System.getInt(context.contentResolver,Settings.System.SCREEN_BRIGHTNESS,128);setBrightness(context,(old*100/255)+delta)}.getOrDefault(false)}
    fun toggleRotation(context:Context):Boolean{if(!Settings.System.canWrite(context)){requestWriteSettings(context);return false};return runCatching{val old=Settings.System.getInt(context.contentResolver,Settings.System.ACCELEROMETER_ROTATION,1);Settings.System.putInt(context.contentResolver,Settings.System.ACCELEROMETER_ROTATION,if(old==1)0 else 1)}.getOrDefault(false)}
    fun toggleDnd(context:Context):Boolean{val nm=context.getSystemService(NotificationManager::class.java);if(!nm.isNotificationPolicyAccessGranted){openDnd(context);return false};return runCatching{val cur=nm.currentInterruptionFilter;nm.setInterruptionFilter(if(cur==NotificationManager.INTERRUPTION_FILTER_NONE)NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_NONE);true}.getOrDefault(false)}
    fun openWifi(context:Context)=context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openBluetooth(context:Context)=context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openAirplane(context:Context)=context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openRotation(context:Context)=context.startActivity(Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openDnd(context:Context)=context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openNotificationAccess(context:Context)=context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun requestWriteSettings(context:Context)=context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun fail(context:Context,message:String)=Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
}
