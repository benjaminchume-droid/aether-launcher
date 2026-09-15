package com.aether.launcher

import android.content.Context
import com.aether.launcher.engine.sensing.AppSensingEngine
import com.aether.launcher.engine.security.SecurityEngine

/** Process-wide runtime. Initialization is idempotent so Activity/services share one engine graph. */
object AetherRuntime {
    private val lock = Any()
    private var initialized = false

    lateinit var registry: EngineRegistry
        private set
    lateinit var sensing: AppSensingEngine
        private set
    lateinit var security: SecurityEngine
        private set

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            registry = EngineRegistry(context.applicationContext)
            sensing = registry.sensing
            security = registry.security
            registry.startAll()
            initialized = true
        }
    }

    fun isInitialized(): Boolean = initialized

    fun shutdown() {
        synchronized(lock) {
            if (!initialized) return
            registry.stopAll()
            initialized = false
        }
    }
}
