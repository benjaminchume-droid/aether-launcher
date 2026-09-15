package com.aether.launcher.engine.control

import com.aether.launcher.engine.*
data class ControlState(val wifi:Boolean=false,val bluetooth:Boolean=false,val brightness:Float=.5f,val volume:Float=.5f)
class ControlCenterEngine: AetherEngine { override val id="control-center"; private var state=EngineHealth.STOPPED; var controls=ControlState(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun setBrightness(v:Float){controls=controls.copy(brightness=v.coerceIn(0f,1f))}; fun setVolume(v:Float){controls=controls.copy(volume=v.coerceIn(0f,1f))} }
