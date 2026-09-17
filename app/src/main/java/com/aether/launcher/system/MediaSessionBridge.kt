package com.aether.launcher.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.island.ActivityType

/**
 * Phase 4: Wire active media sessions into Island + Control Center.
 * Uses MediaSessionManager when notification listener access is granted.
 */
object MediaSessionBridge {

    private var registered = false
    private val handler = Handler(Looper.getMainLooper())

    private val listener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
            publish(controllers.orEmpty())
        }
    }

    fun start(context: Context) {
        if (registered) return
        val msm = context.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(context, AetherNotificationListener::class.java)
        runCatching {
            msm.addOnActiveSessionsChangedListener(listener, component, handler)
            registered = true
            publish(msm.getActiveSessions(component))
        }
    }

    fun stop(context: Context) {
        if (!registered) return
        val msm = context.getSystemService(MediaSessionManager::class.java) ?: return
        runCatching { msm.removeOnActiveSessionsChangedListener(listener) }
        registered = false
    }

    private fun publish(controllers: List<MediaController>) {
        if (!AetherRuntime.isInitialized()) return
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                it.playbackState?.state == PlaybackState.STATE_BUFFERING
        } ?: controllers.firstOrNull()

        if (playing == null) {
            val cur = AetherRuntime.registry.island.activity
            if (cur.type == ActivityType.MEDIA && cur.key.startsWith("media:")) {
                AetherRuntime.registry.island.clearActivity(cur.key)
            }
            AetherRuntime.registry.controlCenter.setMedia("Not Playing", "")
            AetherOverlayService.refreshFromSystem()
            return
        }

        val meta = playing.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Playing"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playing.playbackState?.position ?: 0L
        val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else -1f

        AetherRuntime.registry.controlCenter.setMedia(title, artist)
        AetherRuntime.registry.island.showMedia(
            title = title,
            artist = artist,
            progress = progress,
            key = "media:${playing.packageName}"
        )
        AetherOverlayService.refreshFromSystem()
    }
}
