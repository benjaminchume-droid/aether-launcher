package com.aether.launcher.engine.security

import android.content.Context
import android.util.Base64
import com.aether.launcher.engine.AetherEngine
import com.aether.launcher.engine.EngineHealth
import java.security.MessageDigest
import java.security.SecureRandom

enum class LockState { LOCKED, AUTHENTICATING, UNLOCKED }

data class ProtectedApp(
    val packageName: String,
    val relockOnScreenOff: Boolean = true
)

/**
 * Security engine with:
 * - Protected app list (persisted)
 * - Safe PIN storage (SHA-256 + per-device salt, never plaintext)
 * - Session unlock cache (in-memory only, cleared on lock/screen-off)
 */
class SecurityEngine(private val context: Context? = null) : AetherEngine {

    override val id = "security"
    private var state = EngineHealth.STOPPED
    private val protected = mutableSetOf<String>()
    private val unlocked = mutableSetOf<String>() // session cache only

    private val prefs by lazy {
        context?.getSharedPreferences("aether_security", Context.MODE_PRIVATE)
    }

    override fun start() {
        protected.clear()
        protected += prefs?.getStringSet("protected", emptySet()).orEmpty()
        state = EngineHealth.RUNNING
    }

    override fun stop() {
        state = EngineHealth.STOPPED
        unlocked.clear()
    }

    override fun health() = state

    // ── Protected apps ──

    @Synchronized
    fun protect(pkg: String) {
        if (pkg.isBlank()) return
        protected += pkg
        unlocked -= pkg
        persistProtected()
    }

    @Synchronized
    fun unprotect(pkg: String) {
        protected -= pkg
        unlocked -= pkg
        persistProtected()
    }

    fun protectedApps(): Set<String> = protected.toSet()
    fun isProtected(pkg: String) = pkg in protected

    fun unlock(pkg: String) {
        if (pkg in protected) unlocked += pkg
    }

    fun isUnlocked(pkg: String) = pkg in unlocked
    fun lock(pkg: String) { unlocked -= pkg }
    fun lockAll() { unlocked.clear() }

    private fun persistProtected() {
        prefs?.edit()?.putStringSet("protected", protected.toSet())?.apply()
    }

    // ── Safe PIN (hash only) ──

    fun hasPin(): Boolean =
        !prefs?.getString("pin_hash", null).isNullOrBlank()

    /** Set or change PIN. Stores only salt + SHA-256(salt||pin). */
    fun setPin(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 12) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs?.edit()
            ?.putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            ?.putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            ?.apply()
        return true
    }

    fun clearPin() {
        prefs?.edit()
            ?.remove("pin_salt")
            ?.remove("pin_hash")
            ?.apply()
    }

    /** Verify PIN against stored hash. Constant-time compare. */
    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs?.getString("pin_salt", null) ?: return false
        val hashB64 = prefs?.getString("pin_hash", null) ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(hashB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = hashPin(pin, salt)
        if (actual.size != expected.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expected[i].toInt())
        return diff == 0
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        // Stretch a bit
        var dig = md.digest()
        repeat(999) {
            md.reset()
            md.update(dig)
            md.update(salt)
            dig = md.digest()
        }
        return dig
    }
}
