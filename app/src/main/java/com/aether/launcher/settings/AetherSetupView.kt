package com.aether.launcher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.aether.launcher.AetherRuntime

/**
 * Setup over the REAL wallpaper with glass panels (not a black sheet).
 */
class AetherSetupView(
    context: Context,
    private val onFinished: () -> Unit
) : FrameLayout(context) {

    private val store = AetherSettingsStore(context)
    private var s = store.load()
    private var page = 0
    private val d get() = resources.displayMetrics.density

    private val wallpaper = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        // Full visibility of wallpaper — glass sits on top
        alpha = 1f
        setImageDrawable(
            runCatching { WallpaperManager.getInstance(context).drawable }
                .getOrElse { ColorDrawable(0xFF1A2332.toInt()) }
        )
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                RenderEffect.createBlurEffect(22f, 22f, Shader.TileMode.CLAMP)
            )
        }
    }


    private val welcomeArt = object : View(context) {
        private val artPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun color(value: Long) = value.toInt()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            // Original, scalable painted welcome scene: sunlit alpine valley and a
            // monumental blue-gold serpent. Drawn natively so it ships with the APK.
            artPaint.shader = LinearGradient(0f, 0f, w, h, intArrayOf(0xFF0878C5.toInt(), 0xFF62C9F0.toInt(), 0xFFF6C98A.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, artPaint)
            artPaint.shader = null
            artPaint.color = 0xCCFFFFFF.toInt()
            canvas.drawCircle(w * .78f, h * .52f, w * .13f, artPaint)
            artPaint.color = 0x55FFFFFF
            canvas.drawCircle(w * .78f, h * .52f, w * .19f, artPaint)
            // Cloud banks
            artPaint.color = 0xDDF7FCFF.toInt()
            for (i in 0..5) {
                val x = w * (i / 5f)
                val y = h * (.19f + (i % 3) * .045f)
                canvas.drawOval(x - w*.16f, y, x + w*.13f, y + h*.035f, artPaint)
                canvas.drawCircle(x, y, w*.055f, artPaint)
            }
            val ridge = Path().apply {
                moveTo(0f, h*.61f); lineTo(w*.12f,h*.49f); lineTo(w*.25f,h*.57f)
                lineTo(w*.42f,h*.43f); lineTo(w*.57f,h*.58f); lineTo(w*.71f,h*.46f)
                lineTo(w*.86f,h*.56f); lineTo(w,h*.48f); lineTo(w,h); lineTo(0f,h); close()
            }
            artPaint.shader = LinearGradient(0f,h*.43f,0f,h,0xFF537D76.toInt(),0xFF102F32.toInt(),Shader.TileMode.CLAMP)
            canvas.drawPath(ridge, artPaint); artPaint.shader = null
            // Snowlit distant peaks
            artPaint.color = 0xFFE6F4F4.toInt()
            val peaks = Path().apply {
                moveTo(w*.35f,h*.62f); lineTo(w*.47f,h*.40f); lineTo(w*.53f,h*.49f)
                lineTo(w*.60f,h*.36f); lineTo(w*.71f,h*.60f); close()
            }
            canvas.drawPath(peaks, artPaint)
            artPaint.color = 0xFF739DA4.toInt()
            canvas.drawPath(Path().apply {
                moveTo(w*.47f,h*.40f); lineTo(w*.50f,h*.49f); lineTo(w*.53f,h*.49f); close()
            }, artPaint)
            // Valley, lake and waterfalls
            artPaint.color = 0xFF1B9DB7.toInt()
            canvas.drawOval(w*.48f,h*.69f,w*.92f,h*.82f,artPaint)
            artPaint.color = 0x99D6F8FF.toInt()
            canvas.drawOval(w*.56f,h*.72f,w*.86f,h*.77f,artPaint)
            artPaint.color = 0xFFB8F1FF.toInt()
            artPaint.strokeWidth = w*.009f
            canvas.drawLine(w*.72f,h*.61f,w*.73f,h*.75f,artPaint)
            canvas.drawLine(w*.82f,h*.63f,w*.83f,h*.75f,artPaint)
            // The serpent's long, coiled body.
            val bodyPath = Path().apply {
                moveTo(-w*.10f,h*.89f)
                cubicTo(w*.12f,h*.70f,w*.47f,h*.93f,w*.34f,h*.79f)
                cubicTo(w*.22f,h*.66f,w*.02f,h*.75f,w*.04f,h*.59f)
                cubicTo(w*.06f,h*.46f,w*.10f,h*.35f,w*.19f,h*.31f)
            }
            artPaint.style = Paint.Style.STROKE
            artPaint.strokeWidth = w*.105f
            artPaint.strokeCap = Paint.Cap.ROUND
            artPaint.strokeJoin = Paint.Join.ROUND
            artPaint.color = 0xFF071A31.toInt()
            canvas.drawPath(bodyPath, artPaint)
            artPaint.strokeWidth = w*.079f
            artPaint.shader = LinearGradient(0f,h*.3f,w*.38f,h*.85f, intArrayOf(0xFF183D66.toInt(),0xFF286D91.toInt(),0xFF102943.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawPath(bodyPath, artPaint); artPaint.shader = null
            // Scale highlights following the curve make the body read as plated metal.
            artPaint.strokeWidth = w*.006f
            artPaint.color = 0xA8E3C16C.toInt()
            for (i in 0..25) {
                val x = w * (.02f + (i%7)*.045f)
                val y = h * (.83f - (i/7)*.075f + kotlin.math.sin(i.toDouble()).toFloat()*.012f)
                canvas.drawArc(x,y,x+w*.035f,y+h*.026f,190f,160f,false,artPaint)
            }
            artPaint.style = Paint.Style.FILL
            // Raised serpent head and amber eye
            artPaint.color = 0xFF0A2039.toInt()
            canvas.drawOval(w*.105f,h*.275f,w*.32f,h*.355f,artPaint)
            artPaint.shader = LinearGradient(w*.13f,h*.28f,w*.29f,h*.35f,0xFF327DA1.toInt(),0xFF10233D.toInt(),Shader.TileMode.CLAMP)
            canvas.drawOval(w*.12f,h*.28f,w*.31f,h*.35f,artPaint); artPaint.shader = null
            artPaint.color = 0xFFFFC75A.toInt()
            canvas.drawCircle(w*.267f,h*.306f,w*.012f,artPaint)
            artPaint.color = 0xFF101723.toInt()
            canvas.drawCircle(w*.271f,h*.306f,w*.004f,artPaint)
            // Atmospheric foreground
            artPaint.shader = LinearGradient(0f,h*.76f,0f,h,0x00102030,0xCC06121D.toInt(),Shader.TileMode.CLAMP)
            canvas.drawRect(0f,h*.74f,w,h,artPaint); artPaint.shader = null
            // Elegant luminous greeting.
            artPaint.textAlign = Paint.Align.CENTER
            artPaint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            artPaint.textSize = w*.15f
            artPaint.setShadowLayer(w*.025f,0f,w*.006f,0xDDFFFFFF.toInt())
            artPaint.color = 0xF5FFFFFF.toInt()
            canvas.drawText("Hello", w*.57f, h*.445f, artPaint)
            artPaint.clearShadowLayer()
        }
    }.apply { visibility = View.GONE }

    private val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(36), dp(24), dp(36))
    }

    init {
        addView(wallpaper, LayoutParams(-1, -1))
        addView(welcomeArt, LayoutParams(-1, -1))
        // Light atmosphere only — wallpaper must remain visible
        addView(View(context).apply { setBackgroundColor(0x4D05070C) }, LayoutParams(-1, -1))
        addView(ScrollView(context).apply {
            isFillViewport = true
            addView(body)
        }, LayoutParams(-1, -1))
        render()
    }

    private fun render() {
        welcomeArt.visibility = if (page == 0) View.VISIBLE else View.GONE
        wallpaper.alpha = if (page == 0) 0f else 1f
        body.removeAllViews()
        when (page) {
            0 -> hello()
            1 -> homeChoice()
            2 -> gridAndLook()
            3 -> islandAndQuick()
            4 -> systemLayer()
            5 -> finishPage()
        }
        progress()
        val action = glassButton(if (page == 5) "Enter Aether" else "Continue") {
            if (page == 5) {
                store.save(s.copy(setupComplete = true))
                onFinished()
            } else {
                page++
                render()
            }
        }
        body.addView(View(context), lp(-1, 18))
        body.addView(action, lp(-1, 56))
        if (page > 0) {
            body.addView(TextView(context).apply {
                text = "\u2039  Back"
                textSize = 15f
                setTextColor(0xE6FFFFFF.toInt())
                gravity = Gravity.CENTER
                setOnClickListener { page--; render() }
            }, lp(-1, 48))
        }
    }

    private fun progress() {
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER }
        for (i in 0..5) {
            val v = View(context).apply {
                setBackgroundColor(if (i == page) 0xFFFFFFFF.toInt() else 0x66FFFFFF)
            }
            row.addView(v, LinearLayout.LayoutParams(
                dp(if (i == page) 36 else 14), dp(3)
            ).also { it.setMargins(dp(3), 0, dp(3), 0) })
        }
        body.addView(row, 0, lp(-1, 28))
    }

    private fun hello() {
        body.addView(View(context), lp(-1, 100))
        body.addView(TextView(context).apply {
            text = "hello"
            textSize = 58f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(28f, 0f, 8f, 0xAA000000.toInt())
        }, lp(-1, 90))
        body.addView(TextView(context).apply {
            text = "Welcome to Aether"
            textSize = 17f
            setTextColor(0xF0FFFFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(10f, 0f, 2f, 0x88000000.toInt())
        }, lp(-1, 36))
        body.addView(View(context), lp(-1, 36))
        body.addView(TextView(context).apply {
            text = "A calm, fluid workspace built around your wallpaper,\nyour apps, and the way you move between them."
            textSize = 14f
            setTextColor(0xE0FFFFFF.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setShadowLayer(8f, 0f, 1f, 0x66000000.toInt())
        }, lp(-1, 70))
        body.addView(View(context), lp(-1, 24))
        body.addView(TextView(context).apply {
            text = "Swipe up or tap Continue"
            textSize = 13f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
        }, lp(-1, 40))
    }

    private fun homeChoice() {
        title("How should Home behave?")
        paragraph("This is the only choice that changes your first interaction. Everything stays editable later.")
        option("All apps on Home", "Every installed launcher app lives on paged Home.", HomeMode.ALL_APPS)
        option("Home + App Drawer", "Keep Home clean and open the full library from the drawer.", HomeMode.HOME_AND_DRAWER)
        option("App Drawer only", "Minimal Home with the library as the primary surface.", HomeMode.DRAWER_ONLY)
    }

    private fun gridAndLook() {
        title("Grid & look")
        paragraph("4\u00d74 through 9\u00d79. Icon size, labels, dock and glass depth are all adjustable later.")
        glassPanel("GRID", "${s.grid.columns}\u00d7${s.grid.rows}  \u2022  icon ${s.grid.iconSize}dp  \u2022  labels ${if (s.grid.showLabels) "on" else "off"}")
        glassPanel("DOCK", if (s.dock.enabled) "Enabled  \u2022  ${s.dock.appCount} apps" else "Disabled")
        glassPanel("GLASS", "Opacity ${s.glass.opacity}%  \u2022  blur ${s.glass.blur}  \u2022  depth ${s.glass.depth}")
        stepper("Columns", s.grid.columns, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(columns = v)); store.save(s); render()
        }
        stepper("Rows", s.grid.rows, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(rows = v)); store.save(s); render()
        }
    }

    private fun islandAndQuick() {
        title("Island & Quick Space")
        paragraph("The capsule lives over every app. Quick Space lives on the edge.")
        toggleRow("Dynamic Island", s.island.enabled) {
            s = s.copy(island = s.island.copy(enabled = it)); store.save(s); render()
        }
        toggleRow("Cutout aware", s.island.cutoutAware) {
            s = s.copy(island = s.island.copy(cutoutAware = it)); store.save(s); render()
        }
        toggleRow("Quick Space", s.quickSpace.enabled) {
            s = s.copy(quickSpace = s.quickSpace.copy(enabled = it)); store.save(s); render()
        }
        glassPanel("ISLAND", "Media \u2022 Calls \u2022 Downloads \u2022 Recording \u2022 Notifications \u2022 Reply")
        glassPanel("QUICK SPACE", "Notes \u2022 Recorder \u2022 Calculator \u2022 Screenshot \u2022 Clipboard \u2022 Recents")
    }

    private fun systemLayer() {
        title("Unlock the Aether layer")
        paragraph("These are optional Android capabilities. Enable only what you want.")
        permission("Default Home", "Let Aether become the real Android Home role.") {
            runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        permission("Liquid Island", "Allow Aether to render its floating system layer.") {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }
        }
        permission("Live Activities", "Connect Android notifications to the Island.") {
            runCatching {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        }
        permission("App Lock", "Enable foreground protection and biometric authentication.") {
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
    }

    private fun finishPage() {
        title("You're ready.")
        paragraph("Aether keeps your layout, folders, dock and security choices on-device.")
        glassPanel("STARTING", "${s.homeMode.name.replace('_', ' ')}  \u2022  ${AetherRuntime.registry.launcher.apps().size} apps")
        glassPanel("GESTURES", "Swipe up = Drawer  \u2022  swipe down = Search  \u2022  long-press = Uninstall / Info")
    }

    private fun title(text: String) {
        body.addView(TextView(context).apply {
            this.text = text
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(16f, 0f, 4f, 0xAA000000.toInt())
        }, lp(-1, 64))
    }

    private fun paragraph(text: String) {
        body.addView(TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xF0FFFFFF.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1f)
            setShadowLayer(8f, 0f, 1f, 0x66000000.toInt())
            setPadding(dp(8), 0, dp(8), 0)
        }, lp(-1, 64))
    }

    private fun glassPanel(head: String, detail: String) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = glassDrawable(0x99101820.toInt(), 26f)
            elevation = dp(8).toFloat()
        }
        box.addView(TextView(context).apply {
            text = head
            textSize = 11f
            letterSpacing = 0.1f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
        })
        box.addView(TextView(context).apply {
            text = detail
            textSize = 13f
            setTextColor(0xE6FFFFFF.toInt())
            setPadding(0, dp(5), 0, 0)
        })
        body.addView(box, lp(-1, -2).also { it.setMargins(0, dp(6), 0, dp(6)) })
    }

    private fun option(head: String, detail: String, value: HomeMode) {
        val selected = s.homeMode == value
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = glassDrawable(if (selected) 0xB05B8FEA.toInt() else 0x99101820.toInt(), 26f)
            elevation = dp(6).toFloat()
            setOnClickListener {
                s = s.copy(homeMode = value)
                store.save(s)
                render()
            }
        }
        box.addView(TextView(context).apply {
            text = (if (selected) "\u25CF  " else "\u25CB  ") + head
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        box.addView(TextView(context).apply {
            text = detail
            textSize = 12f
            setTextColor(0xE0FFFFFF.toInt())
            setPadding(dp(22), dp(4), 0, 0)
        })
        body.addView(box, lp(-1, 78).also { it.setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun toggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(12), dp(10))
            background = glassDrawable(0x99101820.toInt(), 22f)
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(context).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        body.addView(row, lp(-1, 56).also { it.setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(10), dp(8))
            background = glassDrawable(0x99101820.toInt(), 22f)
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(context).apply {
            text = "\u2212"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { if (value > min) onChange(value - 1) }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        row.addView(TextView(context).apply {
            text = value.toString()
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(dp(40), dp(42)))
        row.addView(TextView(context).apply {
            text = "+"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { if (value < max) onChange(value + 1) }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        body.addView(row, lp(-1, 56).also { it.setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun permission(head: String, detail: String, click: () -> Unit) {
        val box = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12))
            background = glassDrawable(0x99101820.toInt(), 22f)
            setOnClickListener { click() }
        }
        box.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = head
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            addView(TextView(context).apply {
                text = detail
                textSize = 11f
                setTextColor(0xCCFFFFFF.toInt())
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(TextView(context).apply {
            text = "OPEN"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(56), dp(36)))
        body.addView(box, lp(-1, 68).also { it.setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun glassButton(text: String, onClick: () -> Unit) = TextView(context).apply {
        this.text = text
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = glassDrawable(0x668FE4FF.toInt(), 28f)
        elevation = dp(10).toFloat()
        setOnClickListener { onClick() }
    }

    private fun glassDrawable(base: Int, radius: Float) = GradientDrawable().apply {
        setColor(base)
        cornerRadius = dp(radius.toInt()).toFloat()
        setStroke(dp(1), 0x66FFFFFF)
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    private fun dp(v: Int) = (v * d).toInt()
}
