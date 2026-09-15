package com.aether.launcher.system

import android.app.WallpaperManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aether.launcher.AetherRuntime

class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }
        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrDefault("Protected app")

        setContentView(LockView(this, label))
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        authenticate(pkg, label)
    }

    private fun authenticate(pkg: String, label: String) {
        val manager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            finish()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AetherRuntime.security.unlock(pkg)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish()
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Aether")
                .setSubtitle("Unlock $label")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    companion object {
        const val EXTRA_PACKAGE = "package_name"
    }
}

private class LockView(
    context: android.content.Context,
    private val label: String
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wallpaper = runCatching {
        WallpaperManager.getInstance(context).drawable
    }.getOrNull()

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    22f,
                    22f,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }

    override fun onDraw(c: Canvas) {
        wallpaper?.let {
            it.setBounds(0, 0, width, height)
            it.draw(c)
        }

        paint.style = Paint.Style.FILL
        paint.color = 0xA8000000.toInt()
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val cx = width / 2f
        val cy = height * 0.47f

        paint.color = 0x22FFFFFF
        c.drawCircle(cx, cy, 78f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0x80FFFFFF.toInt()
        c.drawCircle(cx, cy, 38f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.textSize = 30f
        c.drawText("⌁", cx, cy + 10f, paint)

        paint.textSize = 20f
        c.drawText(label, cx, cy + 112f, paint)

        paint.color = 0xBFFFFFFF.toInt()
        paint.textSize = 14f
        c.drawText("Use Face or Fingerprint", cx, cy + 140f, paint)

        paint.textSize = 11f
        c.drawText("Protected by Aether", cx, cy + 170f, paint)
    }
}
