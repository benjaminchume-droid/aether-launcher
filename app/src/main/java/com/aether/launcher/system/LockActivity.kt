package com.aether.launcher.system

import android.os.Bundle
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import com.aether.launcher.AetherRuntime

class LockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg,0)) } catch (_:Exception) { "Protected app" }
        val view = TextView(this).apply { text="◉\n\n$label\n\nProtected by Aether\n\nAuthenticate to continue"; textSize=20f; gravity=17; setPadding(48,48,48,48) }
        setContentView(view)
        authenticate(pkg)
    }

    private fun authenticate(pkg:String) {
        val manager=BiometricManager.from(this)
        if(manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)==BiometricManager.BIOMETRIC_SUCCESS){
            val executor=ContextCompat.getMainExecutor(this)
            val prompt=BiometricPrompt(this,executor,object:BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){AetherRuntime.security.unlock(pkg);finish()}
                override fun onAuthenticationError(errorCode:Int,errString:CharSequence){finish()}
            })
            val info=BiometricPrompt.PromptInfo.Builder().setTitle("Aether Security").setSubtitle("Unlock $pkg").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            prompt.authenticate(info)
        } else finish()
    }
    companion object { const val EXTRA_PACKAGE="package_name" }
}
