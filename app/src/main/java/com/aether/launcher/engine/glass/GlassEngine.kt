package com.aether.launcher.engine.glass

import com.aether.launcher.engine.*
data class GlassMaterial(val opacity: Float=.72f, val blur: Float=28f, val saturation: Float=1.12f, val refraction: Float=.18f, val elevation: Float=8f)
class GlassEngine: AetherEngine { override val id="glass"; private var state=EngineHealth.STOPPED; override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state }
