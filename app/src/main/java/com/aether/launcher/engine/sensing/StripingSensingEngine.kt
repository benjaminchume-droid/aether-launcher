package com.aether.launcher.engine.sensing

import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth
import kotlin.math.abs
import kotlin.math.min

data class OccupiedRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val source: String,
    val priority: Int = 0
)

data class SafeRegion(val left: Float, val top: Float, val right: Float, val bottom: Float)

class StripingSensingEngine : AetherEngine {
    override val id = "striping-sensing"
    private var state = EngineHealth.STOPPED
    private val occupied = mutableListOf<OccupiedRegion>()
    private var safe = SafeRegion(0f, 0f, 1f, 1f)

    override fun start() { state = EngineHealth.RUNNING }
    override fun stop() { state = EngineHealth.STOPPED; occupied.clear(); safe = SafeRegion(0f,0f,1f,1f) }
    override fun health() = state

    @Synchronized fun replace(regions: List<OccupiedRegion>) {
        occupied.clear(); occupied.addAll(regions.sortedByDescending { it.priority }); recomputeSafe()
    }

    @Synchronized fun add(region: OccupiedRegion) {
        occupied.removeAll { it.source == region.source }; occupied.add(region); recomputeSafe()
    }

    @Synchronized fun remove(source: String) { occupied.removeAll { it.source == source }; recomputeSafe() }
    @Synchronized fun regions(): List<OccupiedRegion> = occupied.toList()
    @Synchronized fun safeRegion(): SafeRegion = safe

    @Synchronized
    fun findHorizontalSpan(width: Float, centerY: Float, preferredCenter: Float = 0.5f): Pair<Float, Float> {
        val requested = width.coerceIn(0.04f, 1f)
        val blocked = occupied.filter { centerY in it.top..it.bottom }.sortedBy { it.left }
        val spans = mutableListOf(0f to 1f)
        blocked.forEach { region ->
            val next = mutableListOf<Pair<Float,Float>>()
            spans.forEach { (l,r) ->
                if (region.right <= l || region.left >= r) next += l to r
                else {
                    if (region.left > l) next += l to region.left.coerceAtMost(r)
                    if (region.right < r) next += region.right.coerceAtLeast(l) to r
                }
            }
            spans.clear(); spans.addAll(next)
        }
        val usable = spans.filter { it.second - it.first >= requested }
        if (usable.isEmpty()) return ((preferredCenter - requested/2f).coerceIn(0f,1f-requested)) to ((preferredCenter + requested/2f).coerceIn(requested,1f))
        val best = usable.minByOrNull { abs((it.first + it.second)/2f - preferredCenter) }!!
        val center = ((best.first + best.second)/2f).coerceIn(best.first + requested/2f, best.second - requested/2f)
        return center-requested/2f to min(1f, center+requested/2f)
    }

    private fun recomputeSafe() {
        if (occupied.isEmpty()) { safe = SafeRegion(0f,0f,1f,1f); return }
        val top = occupied.filter { it.top <= 0.18f }.maxByOrNull { it.bottom }
        val bottom = occupied.filter { it.bottom >= 0.82f }.minByOrNull { it.top }
        safe = SafeRegion(0.02f, top?.bottom?.coerceIn(0f,0.45f) ?: 0.02f, 0.98f, bottom?.top?.coerceIn(0.55f,1f) ?: 0.98f)
    }
}
