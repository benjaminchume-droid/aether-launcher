package com.aether.launcher.engine.island

import com.aether.launcher.engine.*
enum class ActivityType { NONE, NOTIFICATION, MEDIA, CALL, TIMER, RECORDING, NAVIGATION }
data class IslandActivity(val type:ActivityType,val title:String,val detail:String="",val priority:Int=0)
data class IslandGeometry(val widthDp:Float=92f,val heightDp:Float=30f,val radiusDp:Float=18f)
class DynamicIslandEngine: AetherEngine { override val id="dynamic-island"; private var state=EngineHealth.STOPPED; var geometry=IslandGeometry(); var activity=IslandActivity(ActivityType.NONE,""); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun setActivity(value:IslandActivity){activity=value}; fun setGeometry(g:IslandGeometry){geometry=g} }
