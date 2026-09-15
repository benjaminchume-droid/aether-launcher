package com.aether.launcher.engine.notes

import com.aether.launcher.engine.*
data class Note(val id:Long=System.currentTimeMillis(),val text:String,val createdAt:Long=System.currentTimeMillis())
class NotesEngine: AetherEngine { override val id="notes"; private var state=EngineHealth.STOPPED; private val notes=mutableListOf<Note>(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun capture(text:String):Note=Note(text=text).also{notes+=it}; fun all()=notes.toList() }
