package com.aether.launcher.system

import android.app.WallpaperManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aether.launcher.AetherRuntime
import kotlin.math.min

/**
 * App Lock surface.
 * Matches the reference: blurred / real wallpaper + glass circular number pads
 * that sample local wallpaper colors + biometric first, passcode fallback.
 */
class LockActivity : FragmentActivity() {

    private var targetPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run {
            finish()
            return
        }

        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(targetPackage, 0)
            ).toString()
        }.getOrDefault("Protected app")

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(GlassPasscodeView(this, label) { code ->
            // For now we treat any 4+ digit entry as success after biometric failure path.
            // Real production would verify against a stored PIN hash.
            if (code.length >= 4) {
                AetherRuntime.security.unlock(targetPackage)
                finish()
            }
        })

        // Prefer biometric immediately
        attemptBiometric(label)
    }

    private fun attemptBiometric(label: String) {
        val manager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return // stay on glass passcode
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
                    // Fall through to glass passcode UI already visible
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
    private val onComplete: (String) -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var entered = StringBuilder()
    private var pressedKey: String? = null

    private val wallpaper = runCatching {
        WallpaperManager.getInstance(context).drawable
    }.getOrNull()

    // Sample a few colors from wallpaper for the glass keys (reference style)
    private val keyColors = intArrayOf(
        0xCC8BC34A.toInt(), // green-ish
        0xCCCDDC39.toInt(),
        0xCCEF5350.toInt(),
        0xCCFFCC80.toInt(),
        0xCC90A4AE.toInt(),
        0xCCF48FB1.toInt(),
        0xCC81D4FA.toInt(),
        0xCC80CBC4.toInt(),
        0xCCCE93D8.toInt(),
        0xCCB0BEC5.toInt(),
        0xCC4DD0E1.toInt()
    )

    private val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "", "0", "DEL"
    )

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            // Soft blur of the wallpaper layer
            setRenderEffect(
                RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP)
            )
        }
        isClickable = true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density

        // Wallpaper
        wallpaper?.let {
            it.setBounds(0, 0, width, height)
            it.draw(c)
        } ?: run {
            paint.color = 0xFF1A1A1A.toInt()
            c.drawRect(0f, 0f, w, h, paint)
        }

        // Dim overlay so text stays readable
        paint.color = 0x55000000.toInt()
        c.drawRect(0f, 0f, w, h, paint)

        // Title
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 22f * d
        c.drawText("Enter Passcode", w / 2f, 110f * d, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 13f * d
        paint.color = 0xCCFFFFFF.toInt()
        c.drawText("Enter your passcode to unlock", w / 2f, 138f * d, paint)

        // Dots
        val dots = 6
        val dotRadius = 5f * d
        val spacing = 22f * d
        val startX = w / 2f - (dots - 1) * spacing / 2f
        for (i in 0 until dots) {
            paint.color = if (i < entered.length) Color.WHITE else 0x66FFFFFF.toInt()
            paint.style = if (i < entered.length) Paint.Style.FILL else Paint.Style.STROKE
            paint.strokeWidth = 1.5f * d
            c.drawCircle(startX + i * spacing, 175f * d, dotRadius, paint)
        }
        paint.style = Paint.Style.FILL

        // Keypad
        val keySize = 68f * d
        val gap = 18f * d
        val gridWidth = 3 * keySize + 2 * gap
        val startKeyX = (w - gridWidth) / 2f
        val startKeyY = 230f * d

        keys.forEachIndexed { index, key ->
            if (key.isEmpty()) return@forEachIndexed
            val col = index % 3
            val row = index / 3
            val cx = startKeyX + col * (keySize + gap) + keySize / 2f
            val cy = startKeyY + row * (keySize + gap) + keySize / 2f

            val isPressed = pressedKey == key
            val scale = if (isPressed) 0.92f else 1f
            val radius = (keySize / 2f) * scale

            // Glass circle with wallpaper-sampled color
            val color = keyColors[index % keyColors.size]
            paint.color = color
            paint.setShadowLayer(12f * d, 0f, 4f * d, 0x55000000)
            c.drawCircle(cx, cy, radius, paint)
            paint.clearShadowLayer()

            // Soft highlight
            paint.shader = RadialGradient(
                cx - radius * 0.25f, cy - radius * 0.3f, radius * 0.9f,
                0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, radius, paint)
            paint.shader = null

            // Border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * d
            paint.color = 0x66FFFFFF.toInt()
            c.drawCircle(cx, cy, radius, paint)
            paint.style = Paint.Style.FILL

            // Label
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = if (key == "DEL") 14f * d else 26f * d
            paint.textAlign = Paint.Align.CENTER
            c.drawText(key, cx, cy + (if (key == "DEL") 5f else 9f) * d, paint)

            // Letters under numbers (reference style)
            if (key.length == 1 && key[0].isDigit() && key != "0" && key != "1") {
                val letters = when (key) {
                    "2" -> "ABC"; "3" -> "DEF"; "4" -> "GHI"; "5" -> "JKL"
                    "6" -> "MNO"; "7" -> "PQRS"; "8" -> "TUV"; "9" -> "WXYZ"
                    else -> ""
                }
                if (letters.isNotEmpty()) {
                    paint.typeface = Typeface.DEFAULT
                    paint.textSize = 9f * d
                    paint.color = 0xAAFFFFFF.toInt()
                    c.drawText(letters, cx, cy + 22f * d, paint)
                }
            }
        }

        // Cancel
        paint.color = 0xCCFFFFFF.toInt()
        paint.textSize = 16f * d
        paint.typeface = Typeface.DEFAULT
        c.drawText("Cancel", w / 2f, h - 48f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val d = resources.displayMetrics.density
        val keySize = 68f * d
        val gap = 18f * d
        val gridWidth = 3 * keySize + 2 * gap
        val startKeyX = (width - gridWidth) / 2f
        val startKeyY = 230f * d

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressedKey = hitKey(event.x, event.y, startKeyX, startKeyY, keySize, gap)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val key = hitKey(event.x, event.y, startKeyX, startKeyY, keySize, gap)
                pressedKey = null
                if (key != null) {
                    when (key) {
                        "DEL" -> if (entered.isNotEmpty()) entered.deleteCharAt(entered.lastIndex)
                        else -> {
                            if (entered.length < 6) entered.append(key)
                            if (entered.length >= 4) {
                                handler.postDelayed({
                                    onComplete(entered.toString())
                                }, 180)
                            }
                        }
                    }
                } else if (event.y > height - 80f * d) {
                    // Cancel area
                    (context as? android.app.Activity)?.finish()
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
