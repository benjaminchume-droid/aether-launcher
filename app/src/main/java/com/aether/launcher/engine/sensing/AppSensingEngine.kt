package com.aether.launcher.engine.sensing

import com.aether.launcher.engine.*
data class AppTransition(val packageName:String, val timestamp:Long=System.currentTimeMillis())
class AppSensingEngine: AetherEngine { override val id="app-sensing"; private var state=EngineHealth.STOPPED; private var listener:((AppTransition)->Unit)?=null; override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun onAppChanged(l:(AppTransition)->Unit){listener=l}; fun publish(pkg:String){listener?.invoke(AppTransition(pkg))} }
