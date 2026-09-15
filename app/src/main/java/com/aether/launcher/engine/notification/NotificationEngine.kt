package com.aether.launcher.engine.notification

import com.aether.launcher.engine.*
data class AetherNotification(val id:Int,val packageName:String,val title:String,val text:String,val timestamp:Long)
class NotificationEngine: AetherEngine { override val id="notifications"; private var state=EngineHealth.STOPPED; private val items=mutableListOf<AetherNotification>(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun add(n:AetherNotification){items.removeAll{it.id==n.id&&it.packageName==n.packageName};items+=n}; fun all()=items.sortedByDescending{it.timestamp} }
