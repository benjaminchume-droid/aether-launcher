package com.aether.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
class AetherActivity: ComponentActivity(){ lateinit var registry:EngineRegistry; override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); registry=EngineRegistry(this); registry.startAll()}; override fun onDestroy(){registry.stopAll();super.onDestroy()} }
