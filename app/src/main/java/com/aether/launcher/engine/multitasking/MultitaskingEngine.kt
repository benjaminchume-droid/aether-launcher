package com.aether.launcher.engine.multitasking

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth

data class FloatingWindow(
    val packageName: String,
    val bounds: Rect,
    val id: String = packageName + System.currentTimeMillis()
)

/**
 * Orchestrates Android-supported split-screen / freeform launches.
 * Only uses public APIs (FLAG_ACTIVITY_LAUNCH_ADJACENT + launch bounds).
 */
class MultitaskingEngine(private val context: Context? = null) : AetherEngine {

    override val id = "multitasking"
    private var state = EngineHealth.STOPPED
    private val windows = mutableListOf<FloatingWindow>()

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED; windows.clear() }
    override fun health() = state

    fun activeWindows(): List<FloatingWindow> = windows.toList()

    fun launchSplit(packageName: String, left: Boolean = true): Boolean {
        val ctx = context ?: return false
        val launch = ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            ?: return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= 24) {
                val dm = ctx.resources.displayMetrics
                val half = dm.widthPixels / 2
                val bounds = if (left) Rect(0, 0, half, dm.heightPixels)
                else Rect(half, 0, dm.widthPixels, dm.heightPixels)
                val opts = ActivityOptions.makeBasic().setLaunchBounds(bounds)
                ctx.startActivity(launch, opts.toBundle())
                windows += FloatingWindow(packageName, bounds)
            } else {
                ctx.startActivity(launch)
            }
            true
        }.getOrDefault(false)
    }

    fun launchFreeform(packageName: String, widthFraction: Float = 0.72f, heightFraction: Float = 0.58f): Boolean {
        val ctx = context ?: return false
        val launch = ctx.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false

        return runCatching {
            if (Build.VERSION.SDK_INT >= 24) {
                val dm = ctx.resources.displayMetrics
                val w = (dm.widthPixels * widthFraction).toInt()
                val h = (dm.heightPixels * heightFraction).toInt()
                val left = (dm.widthPixels - w) / 2
                val top = (dm.heightPixels - h) / 3
                val bounds = Rect(left, top, left + w, top + h)
                val opts = ActivityOptions.makeBasic().setLaunchBounds(bounds)
                ctx.startActivity(launch, opts.toBundle())
                windows += FloatingWindow(packageName, bounds)
            } else {
                ctx.startActivity(launch)
            }
            true
        }.getOrDefault(false)
    }

    fun clear() { windows.clear() }
}
