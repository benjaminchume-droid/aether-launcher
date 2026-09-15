package com.aether.launcher.engine.persistence

import android.content.Context
import com.aether.launcher.engine.*
class PersistenceEngine(private val context:Context): AetherEngine { override val id="persistence"; private var state=EngineHealth.STOPPED; private val prefs get()=context.getSharedPreferences("aether_state",Context.MODE_PRIVATE); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun put(key:String,value:String){prefs.edit().putString(key,value).apply()}; fun get(key:String)=prefs.getString(key,null) }
