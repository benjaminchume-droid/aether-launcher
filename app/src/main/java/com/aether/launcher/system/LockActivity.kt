package com.aether.launcher.system

import android.app.WallpaperManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aether.launcher.AetherRuntime

/**
 * App Lock surface.
 * - Glass circular keypad (reference style)
 * - Safe PIN verification against persisted hash
 * - Tiny Face + Fingerprint icons at bottom → tap triggers biometric
 */
class LockActivity : FragmentActivity() {

    private var targetPackage: String = ""
    private var appLabel: String = "Protected app"
    private lateinit var passcodeView: GlassPasscodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }

        appLabel = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(targetPackage, 0)
            ).toString()
        }.getOrDefault("Protected app")

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        passcodeView = GlassPasscodeView(
            context = this,
            label = appLabel,
            hasPin = AetherRuntime.security.hasPin(),
            onPinEntered = { code -> handlePin(code) },
            onBiometricRequest = { attemptBiometric(appLabel) },
            onCancel = { finish() }
        )
        setContentView(passcodeView)

        // Auto-prompt biometric once
        passcodeView.post { attemptBiometric(appLabel) }
    }

    private fun handlePin(code: String) {
        val security = AetherRuntime.security
        when {
            // First-time: no PIN set → set it if 4+ digits
            !security.hasPin() && code.length >= 4 -> {
                if (security.setPin(code)) {
                    security.unlock(targetPackage)
                    Toast.makeText(this, "Passcode saved securely", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            // Verify against hash
            security.hasPin() && security.verifyPin(code) -> {
                security.unlock(targetPackage)
                finish()
            }
            else -> {
                passcodeView.showError()
                Toast.makeText(this, "Incorrect passcode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attemptBiometric(label: String) {
        val manager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AetherRuntime.security.unlock(targetPackage)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Stay on passcode UI
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

private class GlassPasscodeView(
    context: android.content.Context,
    private val label: String,
    private val hasPin: Boolean,
    private val onPinEntered: (String) -> Unit,
    private val onBiometricRequest: () -> Unit,
    private val onCancel: () -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var entered = StringBuilder()
    private var pressedKey: String? = null
    private var errorFlash = false

    private val wallpaper = runCatching {
        WallpaperManager.getInstance(context).drawable
    }.getOrNull()

    private val keyColors = intArrayOf(
        0xCC8BC34A.toInt(), 0xCCCDDC39.toInt(), 0xCCEF5350.toInt(),
        0xCCFFCC80.toInt(), 0xCC90A4AE.toInt(), 0xCCF48FB1.toInt(),
        0xCC81D4FA.toInt(), 0xCC80CBC4.toInt(), 0xCCCE93D8.toInt(),
        0xCCB0BEC5.toInt(), 0xCC4DD0E1.toInt()
    )

    private val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "", "0", "DEL"
    )

    // Hit targets for biometric icons
    private var faceRect = RectF()
    private var fingerRect = RectF()

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP))
        }
        isClickable = true
    }

    fun showError() {
        entered.clear()
        errorFlash = true
        invalidate()
        handler.postDelayed({
            errorFlash = false
            invalidate()
        }, 600)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density

        wallpaper?.let {
            it.setBounds(0, 0, width, height)
            it.draw(c)
        } ?: run {
            paint.color = 0xFF1A1A1A.toInt()
            c.drawRect(0f, 0f, w, h, paint)
        }

        paint.color = 0x55000000.toInt()
        c.drawRect(0f, 0f, w, h, paint)

        // Title
        paint.style = Paint.Style.FILL
        paint.color = if (errorFlash) 0xFFFF6B6B.toInt() else Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 22f * d
        c.drawText(
            if (!hasPin) "Set Passcode" else "Enter Passcode",
            w / 2f, 100f * d, paint
        )

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 13f * d
        paint.color = 0xCCFFFFFF.toInt()
        c.drawText(
            if (!hasPin) "Choose a 4–6 digit passcode for $label"
            else "Enter your passcode to unlock",
            w / 2f, 128f * d, paint
        )

        // Dots
        val dots = 6
        val dotRadius = 5f * d
        val spacing = 22f * d
        val startX = w / 2f - (dots - 1) * spacing / 2f
        for (i in 0 until dots) {
            paint.color = when {
                errorFlash -> 0xFFFF6B6B.toInt()
                i < entered.length -> Color.WHITE
                else -> 0x66FFFFFF.toInt()
            }
            paint.style = if (i < entered.length) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = 1.5f * d
            c.drawCircle(startX + i * spacing, 165f * d, dotRadius, paint)
        }
        paint.style = Paint.Style.FILL

        // Keypad
        val keySize = 66f * d
        val gap = 16f * d
        val gridWidth = 3 * keySize + 2 * gap
        val startKeyX = (w - gridWidth) / 2f
        val startKeyY = 210f * d

        keys.forEachIndexed { index, key ->
            if (key.isEmpty()) return@forEachIndexed
            val col = index % 3
            val row = index / 3
            val cx = startKeyX + col * (keySize + gap) + keySize / 2f
            val cy = startKeyY + row * (keySize + gap) + keySize / 2f
            val isPressed = pressedKey == key
            val scale = if (isPressed) 0.92f else 1f
            val radius = (keySize / 2f) * scale

            val color = keyColors[index % keyColors.size]
            paint.color = color
            paint.setShadowLayer(12f * d, 0f, 4f * d, 0x55000000)
            c.drawCircle(cx, cy, radius, paint)
            paint.clearShadowLayer()

            paint.shader = RadialGradient(
                cx - radius * 0.25f, cy - radius * 0.3f, radius * 0.9f,
                0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, radius, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * d
            paint.color = 0x66FFFFFF.toInt()
            c.drawCircle(cx, cy, radius, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = if (key == "DEL") 13f * d else 25f * d
            paint.textAlign = Paint.Align.CENTER
            c.drawText(key, cx, cy + (if (key == "DEL") 5f else 9f) * d, paint)

            if (key.length == 1 && key[0].isDigit() && key != "0" && key != "1") {
                val letters = when (key) {
                    "2" -> "ABC"; "3" -> "DEF"; "4" -> "GHI"; "5" -> "JKL"
                    "6" -> "MNO"; "7" -> "PQRS"; "8" -> "TUV"; "9" -> "WXYZ"
                    else -> ""
                }
                if (letters.isNotEmpty()) {
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 8.5f * d
                    paint.color = 0xAAFFFFFF.toInt()
                    c.drawText(letters, cx, cy + 21f * d, paint)
                }
            }
        }

        // ── Face + Fingerprint icons at bottom ──
        val iconY = h - 110f * d
        val iconR = 22f * d
        val faceCx = w / 2f - 48f * d
        val fingerCx = w / 2f + 48f * d

        faceRect.set(faceCx - iconR - 8f * d, iconY - iconR - 8f * d, faceCx + iconR + 8f * d, iconY + iconR + 8f * d)
        fingerRect.set(fingerCx - iconR - 8f * d, iconY - iconR - 8f * d, fingerCx + iconR + 8f * d, iconY + iconR + 8f * d)

        // Face icon circle
        paint.color = 0x55FFFFFF.toInt()
        paint.style = Paint.Style.FILL
        c.drawCircle(faceCx, iconY, iconR, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * d
        paint.color = 0xAAFFFFFF.toInt()
        c.drawCircle(faceCx, iconY, iconR, paint)
        // Simple face glyph
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 18f * d
        paint.textAlign = Paint.Align.CENTER
        c.drawText("😐", faceCx, iconY + 6f * d, paint)

        // Fingerprint icon circle
        paint.color = 0x55FFFFFF.toInt()
        paint.style = Paint.Style.FILL
        c.drawCircle(fingerCx, iconY, iconR, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * d
        paint.color = 0xAAFFFFFF.toInt()
        c.drawCircle(fingerCx, iconY, iconR, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 18f * d
        c.drawText("🖐", fingerCx, iconY + 6f * d, paint)

        // Labels under icons
        paint.textSize = 10f * d
        paint.color = 0x99FFFFFF.toInt()
        c.drawText("Face", faceCx, iconY + iconR + 16f * d, paint)
        c.drawText("Touch", fingerCx, iconY + iconR + 16f * d, paint)

        // Cancel
        paint.color = 0xCCFFFFFF.toInt()
        paint.textSize = 15f * d
        c.drawText("Cancel", w / 2f, h - 28f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val d = resources.displayMetrics.density
        val keySize = 66f * d
        val gap = 16f * d
        val gridWidth = 3 * keySize + 2 * gap
        val startKeyX = (width - gridWidth) / 2f
        val startKeyY = 210f * d

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressedKey = hitKey(event.x, event.y, startKeyX, startKeyY, keySize, gap)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val key = hitKey(event.x, event.y, startKeyX, startKeyY, keySize, gap)
                pressedKey = null

                when {
                    key != null -> {
                        when (key) {
                            "DEL" -> if (entered.isNotEmpty()) entered.deleteCharAt(entered.lastIndex)
                            else -> {
                                if (entered.length < 6) entered.append(key)
                                if (entered.length >= 4) {
                                    handler.postDelayed({
                                        onPinEntered(entered.toString())
                                    }, 160)
                                }
                            }
                        }
                    }
                    faceRect.contains(event.x, event.y) || fingerRect.contains(event.x, event.y) -> {
                        onBiometricRequest()
                    }
                    event.y > height - 50f * d -> onCancel()
                }
                invalidate()
            }
        }
        return true
    }

    private fun hitKey(
        x: Float, y: Float,
        startX: Float, startY: Float,
        size: Float, gap: Float
    ): String? {
        keys.forEachIndexed { index, key ->
            if (key.isEmpty()) return@forEachIndexed
            val col = index % 3
            val row = index / 3
            val cx = startX + col * (size + gap) + size / 2f
            val cy = startY + row * (size + gap) + size / 2f
            val dx = x - cx
            val dy = y - cy
            if (dx * dx + dy * dy <= (size / 2f) * (size / 2f)) return key
        }
        return null
    }
}
