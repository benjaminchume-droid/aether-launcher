package com.aether.launcher.engine.security

import android.content.Context
import com.aether.launcher.engine.*

enum class LockState { LOCKED, AUTHENTICATING, UNLOCKED }
data class ProtectedApp(val packageName: String, val relockOnScreenOff: Boolean = true)

class SecurityEngine(private val context: Context? = null) : AetherEngine {
    override val id = "security"
    private var state = EngineHealth.STOPPED
    private val protected = mutableSetOf<String>()
    private val unlocked = mutableSetOf<String>()
    private val prefs by lazy { context?.getSharedPreferences("aether_security", Context.MODE_PRIVATE) }
    override fun start() {
        protected.clear()
        protected += prefs?.getStringSet("protected", emptySet()).orEmpty()
        state = EngineHealth.RUNNING
    }
    override fun stop() { state = EngineHealth.STOPPED; unlocked.clear() }
    override fun health() = state
    @Synchronized fun protect(pkg: String) { if (pkg.isBlank()) return; protected += pkg; unlocked -= pkg; persist() }
    @Synchronized fun unprotect(pkg: String) { protected -= pkg; unlocked -= pkg; persist() }
    fun protectedApps(): Set<String> = protected.toSet()
    fun isProtected(pkg: String) = pkg in protected
    fun unlock(pkg: String) { if (pkg in protected) unlocked += pkg }
    fun isUnlocked(pkg: String) = pkg in unlocked
    fun lock(pkg: String) { unlocked -= pkg }
    fun lockAll() { unlocked.clear() }
    private fun persist() { prefs?.edit()?.putStringSet("protected", protected.toSet())?.apply() }
}
