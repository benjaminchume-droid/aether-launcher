package com.aether.launcher.engine.security

import com.aether.launcher.engine.*
enum class LockState { LOCKED, AUTHENTICATING, UNLOCKED }
data class ProtectedApp(val packageName:String, val relockOnScreenOff:Boolean=true)
class SecurityEngine: AetherEngine { override val id="security"; private var state=EngineHealth.STOPPED; private val protected=mutableSetOf<String>(); private val unlocked=mutableSetOf<String>(); override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state; fun protect(pkg:String){protected+=pkg; unlocked-=pkg}; fun unprotect(pkg:String){protected-=pkg; unlocked-=pkg}; fun isProtected(pkg:String)=pkg in protected; fun unlock(pkg:String){if(pkg in protected) unlocked+=pkg}; fun isUnlocked(pkg:String)=pkg in unlocked; fun lockAll(){unlocked.clear()} }
