package com.aether.launcher.engine.physics

import com.aether.launcher.engine.*
data class Spring(val stiffness: Float=420f, val damping: Float=32f, var position: Float=0f, var velocity: Float=0f) { fun step(target: Float, dt: Float): Float { val a=stiffness*(target-position)-damping*velocity; velocity+=a*dt; position+=velocity*dt; return position } }
data class GestureSample(val x:Float,val y:Float,val timeMs:Long)
class PhysicsEngine: AetherEngine { override val id="physics"; private var state=EngineHealth.STOPPED; override fun start(){state=EngineHealth.RUNNING}; override fun stop(){state=EngineHealth.STOPPED}; override fun health()=state }
