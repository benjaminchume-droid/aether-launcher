package com.aether.launcher.engine.notification

import com.aether.launcher.engine.*

data class AetherNotification(val id: Int, val packageName: String, val title: String, val text: String, val timestamp: Long)

class NotificationEngine : AetherEngine {
    override val id = "notifications"
    private var state = EngineHealth.STOPPED
    private val items = mutableListOf<AetherNotification>()

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED; items.clear() }
    override fun health() = state

    @Synchronized fun add(n: AetherNotification) {
        items.removeAll { it.id == n.id && it.packageName == n.packageName }
        items += n
        if (items.size > 100) items.sortByDescending { it.timestamp }.also { while (items.size > 100) items.removeAt(items.lastIndex) }
    }

    @Synchronized fun remove(id: Int, packageName: String) { items.removeAll { it.id == id && it.packageName == packageName } }
    @Synchronized fun all(): List<AetherNotification> = items.sortedByDescending { it.timestamp }
    @Synchronized fun latest(): AetherNotification? = items.maxByOrNull { it.timestamp }
}
