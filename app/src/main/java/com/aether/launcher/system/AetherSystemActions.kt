package com.aether.launcher.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast

object AetherSystemActions {
    fun launchHome(context: Context) { context.startActivity(Intent(context, com.aether.launcher.AetherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
    fun openSettings(context: Context) { context.startActivity(Intent(context, com.aether.launcher.settings.AetherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun openSurface(context: Context, surface: String) { context.startActivity(Intent(context, com.aether.launcher.ui.AetherSurfaceActivity::class.java).putExtra("surface", surface).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun launchPackage(context: Context, packageName: String): Boolean = runCatching { context.packageManager.getLaunchIntentForPackage(packageName)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } ?: false }.getOrDefault(false)
    fun toggleTorch(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(CameraManager::class.java)
        val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return false
        val state = context.getSharedPreferences("aether_controls", Context.MODE_PRIVATE).getBoolean("torch", false)
        manager.setTorchMode(id, !state); context.getSharedPreferences("aether_controls", Context.MODE_PRIVATE).edit().putBoolean("torch", !state).apply(); true
    }.getOrDefault(false)
    fun volume(context: Context, direction: Int) { context.getSystemService(AudioManager::class.java).adjustVolume(direction, AudioManager.FLAG_SHOW_UI) }
    fun openWifi(context: Context) = context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openBluetooth(context: Context) = context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openAirplane(context: Context) = context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openRotation(context: Context) = context.startActivity(Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openDnd(context: Context) = context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun requestWriteSettings(context: Context) = context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun fail(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
