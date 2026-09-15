package com.aether.launcher.engine.capture

import com.aether.launcher.engine.*
enum class CaptureState { IDLE, RECORDING, SAVING }
class AudioCaptureEngine: AetherEngine { override val id="audio-capture"; private var state=EngineHealth.STOPPED; var capture=CaptureState.IDLE; override fun start(){state=EngineHealth.RUNNING}; override fun stop(){capture=CaptureState.IDLE;state=EngineHealth.STOPPED}; override fun health()=state; fun begin(){if(state==EngineHealth.RUNNING)capture=CaptureState.RECORDING}; fun end(){if(capture==CaptureState.RECORDING)capture=CaptureState.SAVING} }
