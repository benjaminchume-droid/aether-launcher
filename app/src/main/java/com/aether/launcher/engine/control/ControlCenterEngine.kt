package com.aether.launcher.engine.control

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth
import com.aether.launcher.system.AetherSystemActions

data class ControlState(
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val airplane: Boolean = false,
    val rotation: Boolean = true,
    val dnd: Boolean = false,
    val torch: Boolean = false,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val mediaTitle: String = "Not Playing",
    val mediaArtist: String = ""
)

/**
 * Real control state.
 * Reads / writes actual Android settings where permitted.
 * UI only reflects this state — never invents it.
 */
class ControlCenterEngine(private val context: Context? = null) : AetherEngine {

    override val id = "control-center"
    private var state = EngineHealth.STOPPED
    var controls = ControlState()
        private set

    override fun start() {
        refresh()
        state = EngineHealth.RUNNING
    }

    override fun stop() {
        state = EngineHealth.STOPPED
    }

    override fun health() = state

    fun refresh() {
        val ctx = context ?: return
        val am = ctx.getSystemService(AudioManager::class.java)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        val brightness = runCatching {
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        }.getOrDefault(0.5f)

        val rotation = runCatching {
            Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        }.getOrDefault(true)

        val torch = ctx.getSharedPreferences("aether_controls", Context.MODE_PRIVATE)
            .getBoolean("torch", false)

        controls = controls.copy(
            brightness = brightness.coerceIn(0f, 1f),
            volume = (curVol.toFloat() / maxVol).coerceIn(0f, 1f),
            rotation = rotation,
            torch = torch
        )
    }

    fun setBrightness(v: Float) {
        controls = controls.copy(brightness = v.coerceIn(0f, 1f))
        context?.let { AetherSystemActions.setBrightness(it, (v * 100).toInt()) }
    }

    fun setVolume(v: Float) {
        controls = controls.copy(volume = v.coerceIn(0f, 1f))
        val ctx = context ?: return
        val am = ctx.getSystemService(AudioManager::class.java)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).toInt(), 0)
    }

    fun toggleTorch(): Boolean {
        val ctx = context ?: return false
        val ok = AetherSystemActions.toggleTorch(ctx)
        if (ok) {
            controls = controls.copy(torch = !controls.torch)
        }
        return ok
    }

    fun toggleRotation(): Boolean {
        val ctx = context ?: return false
        val ok = AetherSystemActions.toggleRotation(ctx)
        if (ok) {
            controls = controls.copy(rotation = !controls.rotation)
        }
        return ok
    }

    fun setMedia(title: String, artist: String = "") {
        controls = controls.copy(mediaTitle = title, mediaArtist = artist)
    }
}
