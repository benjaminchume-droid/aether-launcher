package com.aether.launcher.engine.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.aether.launcher.engine.*
data class AppInfo(val packageName: String, val label: String)
class LauncherEngine(private val context: Context): AetherEngine {
    override val id = "launcher"
    private var state = EngineHealth.STOPPED
    private val pm get() = context.packageManager
    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED }
    override fun health() = state
    fun apps(): List<AppInfo> = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    fun icon(packageName: String): Drawable? = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)
}
