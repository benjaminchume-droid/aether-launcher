package com.aether.launcher

import android.content.Context
import com.aether.launcher.engine.sensing.AppSensingEngine
import com.aether.launcher.engine.security.SecurityEngine

object AetherRuntime {
    lateinit var registry: EngineRegistry
    lateinit var sensing: AppSensingEngine
    lateinit var security: SecurityEngine

    fun initialize(context: Context) {
        registry = EngineRegistry(context.applicationContext)
        sensing = registry.sensing
        security = registry.security
        registry.startAll()
    }
}
