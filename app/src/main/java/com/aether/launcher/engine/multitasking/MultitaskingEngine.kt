package com.aether.launcher.engine.multitasking

import com.aether.launcher.engine.*
enum class WindowMode { FULLSCREEN, SPLIT, FLOATING, MINI }
data class AetherWindow(val id:String,val packageName:String,val x:Int=0,val y:Int=0,val width:Int=600,val height:Int=900,val mode:WindowMode=WindowMode.FLOATING)
class MultitaskingEngine: AetherEngine { override val id="multitasking"; private var state=EngineHealth.STOPPED; private val windows=mutableMapOf<String,AetherWindow>(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun add(w:AetherWindow){windows[w.id]=w}; fun resize(id:String,w:Int,h:Int){windows[id]?.let{windows[id]=it.copy(width=w.coerceAtLeast(160),height=h.coerceAtLeast(160))}}; fun all()=windows.values.toList() }
