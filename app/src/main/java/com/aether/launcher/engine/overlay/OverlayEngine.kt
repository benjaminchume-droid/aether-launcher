package com.aether.launcher.engine.overlay

import com.aether.launcher.engine.*
data class OverlayState(val visible:Boolean=true,val edge:String="right",val expansion:Float=0f)
class OverlayEngine: AetherEngine { override val id="overlay"; private var state=EngineHealth.STOPPED; var overlay=OverlayState(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun expand(amount:Float){overlay=overlay.copy(expansion=amount.coerceIn(0f,1f))} }
