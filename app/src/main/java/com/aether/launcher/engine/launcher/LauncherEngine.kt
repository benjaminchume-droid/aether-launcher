package com.aether.launcher.engine.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth
import java.util.concurrent.ConcurrentHashMap

data class AppInfo(val packageName: String, val label: String)

class LauncherEngine(private val context: Context) : AetherEngine {
    override val id = "launcher"
    private var state = EngineHealth.STOPPED
    private val pm get() = context.packageManager
    private val iconCache = ConcurrentHashMap<String, Drawable>()
    private var appCache: List<AppInfo> = emptyList()

    override fun start() { state = EngineHealth.RUNNING; refresh() }
    override fun stop() { state = EngineHealth.STOPPED; iconCache.clear(); appCache = emptyList() }
    override fun health() = state

    @Synchronized
    fun refresh() {
        appCache = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun apps(): List<AppInfo> = appCache.ifEmpty { refresh(); appCache }

    fun icon(packageName: String): Drawable? =
        iconCache[packageName] ?: runCatching { pm.getApplicationIcon(packageName) }
            .getOrNull()?.also { iconCache[packageName] = it }

    fun clearIconCache() = iconCache.clear()

    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun launch(packageName: String): Boolean =
        runCatching {
            val intent = launchIntent(packageName) ?: return false
            context.startActivity(intent)
            true
        }.getOrDefault(false)
}
