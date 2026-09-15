package com.aether.launcher.system

import android.os.Bundle
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aether.launcher.AetherRuntime

class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) } catch (_: Exception) { "Protected app" }
        setContentView(TextView(this).apply {
            text = "◉\n\n$label\n\nProtected by Aether\n\nAuthenticate to continue"
            textSize = 20f
            gravity = 17
            setPadding(48, 48, 48, 48)
        })
        authenticate(pkg)
    }

    private fun authenticate(pkg: String) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            finish()
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                AetherRuntime.security.unlock(pkg)
                finish()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { finish() }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Aether Security")
            .setSubtitle("Unlock $labelForPrompt")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private val labelForPrompt: String
        get() = intent.getStringExtra(EXTRA_PACKAGE) ?: "Protected app"

    companion object { const val EXTRA_PACKAGE = "package_name" }
}
