package com.aether.launcher.engine

interface AetherEngine { val id: String; fun start(); fun stop(); fun health(): EngineHealth }
enum class EngineHealth { STOPPED, STARTING, RUNNING, DEGRADED, FAILED }
