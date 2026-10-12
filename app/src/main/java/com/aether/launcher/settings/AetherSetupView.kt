package com.aether.launcher.settings

import android.app.Activity
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

    private val welcomeArt = ImageView(context).apply {
        // Full-bleed welcome artwork (the "Hello" landscape + serpent).
        // CENTER_CROP adapts it cleanly to any portrait phone.
        // Safe load: never crash the build/runtime if the drawable is missing.
        scaleType = ImageView.ScaleType.CENTER_CROP
        val art = runCatching {
            context.getDrawable(com.aether.launcher.R.drawable.aether_welcome)
        }.getOrNull()
        if (art != null) {
            setImageDrawable(art)
        } else {
            // Soft dark gradient fallback so the screen still looks intentional
            setBackgroundColor(0xFF0B1A2A.toInt())
        }
        contentDescription = "Aether Launcher welcome artwork"
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(36), dp(24), dp(36))
    }

    // Bottom-right Continue / Enter button (used especially on the welcome page)
    private var actionButton: TextView? = null

    init {
        addView(wallpaper, LayoutParams(-1, -1))
        addView(welcomeArt, LayoutParams(-1, -1))
        // Light atmosphere only — wallpaper / art must remain visible
        addView(View(context).apply { setBackgroundColor(0x3305070C) }, LayoutParams(-1, -1))
        addView(ScrollView(context).apply {
            isFillViewport = true
            addView(body)
        }, LayoutParams(-1, -1))
        render()
    }

    private fun render() {
        welcomeArt.visibility = if (page == 0) View.VISIBLE else View.GONE
        wallpaper.alpha = if (page == 0) 0f else 1f

        // Remove previous floating action if any
        actionButton?.let { removeView(it) }
        actionButton = null

        body.removeAllViews()
        when (page) {
            0 -> hello()
            1 -> homeChoice()
            2 -> gridAndLook()
            3 -> islandAndQuick()
            4 -> systemLayer()
            5 -> finishPage()
        }

        // Progress dots only on pages after welcome
        if (page > 0) {
            progress()
        }

        val label = if (page == 5) "Enter Aether" else "Continue"
        val action = glassButton(label) {
            if (page == 5) {
                store.save(s.copy(setupComplete = true))
                onFinished()
            } else {
                page++
                render()
            }
        }

        if (page == 0) {
            // Welcome page uses the same composed onboarding layout as the reference:
            // brand at the top, glass information card in the lower-middle, paired
            // Back/Continue pills near the bottom.
            body.addView(View(context), lp(-1, dp(8)))
            val controls = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val back = glassButton("\u2190   Back") {
                // Return through the hosting activity's normal navigation without
                // marking setup complete.
                (context as? Activity)?.onBackPressedDispatcher?.onBackPressed()
            }.apply {
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            controls.addView(back, LinearLayout.LayoutParams(0, dp(56), 0.82f).also {
                it.setMargins(0, 0, dp(12), 0)
            })
            controls.addView(action, LinearLayout.LayoutParams(0, dp(60), 1.18f))
            body.addView(controls, lp(-1, dp(64)))
        } else {
            body.addView(View(context), lp(-1, 18))
            body.addView(action, lp(-1, 56))
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
        // Match the supplied setup screenshot rather than the earlier centered
        // title/button layout: compact Aether brand at top, floating glass card
        // in the lower-middle, progress indicator inside the card, paired actions.
        body.addView(View(context), lp(-1, dp(12)))
        body.addView(TextView(context).apply {
            text = "\u221e"
            textSize = 82f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(0xFF83DFFF.toInt())
            gravity = Gravity.CENTER
            setShadowLayer(20f, 0f, 0f, 0xAA548DFF.toInt())
        }, lp(-1, dp(78)))
        body.addView(TextView(context).apply {
            text = "A e t h e r"
            textSize = 27f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.08f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(8f, 0f, 2f, 0xAA000000.toInt())
        }, lp(-1, dp(38)))
        body.addView(TextView(context).apply {
            text = "L A U N C H E R"
            textSize = 10f
            letterSpacing = 0.20f
            setTextColor(0xDDEAF8FF.toInt())
            gravity = Gravity.CENTER
        }, lp(-1, dp(24)))

        body.addView(View(context), lp(-1, (resources.displayMetrics.heightPixels * 0.30f).toInt()))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28), dp(24), dp(28), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xAA18283D.toInt(), 0x774E7895.toInt(), 0x99303E57.toInt())
            ).apply {
                cornerRadius = dp(42).toFloat()
                setStroke(dp(1), 0x99B8E8FF.toInt())
            }
            elevation = dp(14).toFloat()
        }
        card.addView(TextView(context).apply {
            text = "Welcome to Aether"
            textSize = 25f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setTextColor(0xFFF3F7FF.toInt())
            setShadowLayer(6f, 0f, 1f, 0x99000000.toInt())
        }, lp(-1, dp(42)))
        card.addView(TextView(context).apply {
            text = "A calm, fluid workspace built around your wallpaper, your apps, and the way you move."
            textSize = 14f
            setTextColor(0xE6F1F6FF.toInt())
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(4), 0, dp(8))
            setShadowLayer(4f, 0f, 1f, 0x66000000.toInt())
        }, lp(-1, dp(58)))
        val progressRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(View(context).apply {
                background = GradientDrawable().apply {
                    setColor(0xFF9CEBFF.toInt())
                    cornerRadius = dp(4).toFloat()
                }
            }, LinearLayout.LayoutParams(dp(38), dp(4)).also { it.setMargins(0, 0, dp(12), 0) })
            for (i in 1..5) addView(View(context).apply {
                background = GradientDrawable().apply {
                    setColor(0x66FFFFFF)
                    cornerRadius = dp(4).toFloat()
                }
            }, LinearLayout.LayoutParams(dp(6), dp(6)).also { it.setMargins(dp(4), 0, dp(4), 0) })
        }
        card.addView(progressRow, lp(-1, dp(18)))
        body.addView(card, lp(-1, dp(198)))
        body.addView(View(context), lp(-1, dp(64)))
    }

    private fun homeChoice() {
        title("How should Home work?")
        paragraph("This is the only choice that changes your first interaction. Everything stays editable later.")
        option("Home screen only", "Keep your apps on Home. App Drawer is disabled.", HomeMode.ALL_APPS)
        option("Home screen and App Drawer", "Use Home normally and swipe up to open all apps.", HomeMode.HOME_AND_DRAWER)
        option("App Drawer only", "Keep Home minimal and use the drawer to open your apps.", HomeMode.DRAWER_ONLY)
    }

    private fun gridAndLook() {
        title("Grid & look")
        paragraph("4×4 through 9×9. Icon size, labels, dock and glass depth are adjustable later.")

        glassPanel("GRID", "${s.grid.columns}×${s.grid.rows}  •  icon ${s.grid.iconSize}dp  •  labels ${if (s.grid.showLabels) "on" else "off"}").apply {
            isClickable = true
            setOnClickListener { showGridOptions() }
        }
        glassPanel("DOCK", if (s.dock.enabled) "Enabled  •  ${s.dock.appCount} apps" else "Disabled").apply {
            isClickable = true
            setOnClickListener { showDockEditor() }
        }
        glassPanel("GLASS", "Opacity ${s.glass.opacity}%  •  blur ${s.glass.blur}  •  depth ${s.glass.depth}").apply {
            isClickable = true
            setOnClickListener { showGlassEditor() }
        }
        stepper("Columns", s.grid.columns, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(columns = v)); store.save(s); render()
        }
        stepper("Rows", s.grid.rows, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(rows = v)); store.save(s); render()
        }
    }

    private fun showGridOptions() {
        val labels = arrayOf("Show app labels", "Use compact icon spacing")
        val checked = booleanArrayOf(s.grid.showLabels, s.grid.iconSize <= 56)
        android.app.AlertDialog.Builder(context)
            .setTitle("Grid & icons")
            .setMultiChoiceItems(labels, checked) { _, which, value ->
                s = if (which == 0) s.copy(grid = s.grid.copy(showLabels = value))
                    else s.copy(grid = s.grid.copy(iconSize = if (value) 52 else 64))
                store.save(s)
            }
            .setPositiveButton("Done") { _, _ -> render() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDockEditor() {
        val apps = AetherRuntime.registry.launcher.apps().sortedBy { it.label.lowercase() }
        if (apps.isEmpty()) {
            android.app.AlertDialog.Builder(context).setTitle("Dock apps")
                .setMessage("No launchable apps were found on this device.")
                .setPositiveButton("OK", null).show()
            return
        }
        val dockStore = com.aether.launcher.AetherDockStore(context)
        val existing = dockStore.load().filter { pkg -> apps.any { it.packageName == pkg } }.toMutableList()
        if (existing.isEmpty()) existing.addAll(apps.shuffled().take(s.dock.appCount.coerceIn(3, 7)).map { it.packageName })
        val selected = apps.map { it.packageName in existing }.toBooleanArray()
        val names = apps.map { it.label.toString() }.toTypedArray()
        val dialog = android.app.AlertDialog.Builder(context)
            .setTitle("Choose up to ${s.dock.appCount} dock apps")
            .setMultiChoiceItems(names, selected) { d, which, checked ->
                val count = selected.count { it }
                if (checked && count > s.dock.appCount.coerceIn(3, 7)) {
                    selected[which] = false
                    (d as? android.app.AlertDialog)?.listView?.setItemChecked(which, false)
                    Toast.makeText(context, "The dock holds ${s.dock.appCount} apps. Change the app count first.", Toast.LENGTH_SHORT).show()
                } else selected[which] = checked
            }
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val chosen = apps.indices.filter { selected[it] }.map { apps[it].packageName }
                if (chosen.isEmpty()) {
                    Toast.makeText(context, "Select at least one app.", Toast.LENGTH_SHORT).show()
                } else {
                    dockStore.save(chosen)
                    s = s.copy(dock = s.dock.copy(appCount = chosen.size, enabled = true))
                    store.save(s)
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }

    private fun showGlassEditor() {
        val opacity = SeekBar(context).apply { max = 100; progress = s.glass.opacity }
        val blur = SeekBar(context).apply { max = 40; progress = s.glass.blur }
        val depth = SeekBar(context).apply { max = 18; progress = s.glass.depth.coerceIn(0, 18) }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(4))
        }
        fun addSlider(label: String, bar: SeekBar) {
            panel.addView(TextView(context).apply { text = "$label: ${bar.progress}"; setTextColor(Color.WHITE); textSize = 14f })
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    (panel.getChildAt(panel.indexOfChild(bar) - 1) as? TextView)?.text = "$label: $value"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            panel.addView(bar, LinearLayout.LayoutParams(-1, dp(42)))
        }
        addSlider("Opacity (%)", opacity)
        addSlider("Blur", blur)
        addSlider("Depth", depth)
        panel.setBackgroundColor(0xDD18283D.toInt())
        android.app.AlertDialog.Builder(context).setTitle("Liquid glass").setView(panel)
            .setPositiveButton("Apply") { _, _ ->
                s = s.copy(glass = s.glass.copy(opacity = opacity.progress.coerceIn(20, 100), blur = blur.progress, depth = depth.progress.coerceIn(2, 18)))
                store.save(s)
                applyGlassSettings()
                render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun applyGlassSettings() {
        if (!AetherRuntime.isInitialized()) return
        AetherRuntime.registry.glass.update(
            com.aether.launcher.engine.glass.GlassMaterial(
                opacity = s.glass.opacity / 100f,
                blurRadius = s.glass.blur.toFloat(),
                saturation = s.glass.saturation / 100f,
                refraction = s.glass.refraction / 100f,
                elevation = s.glass.depth.toFloat(),
                highlight = s.glass.highlight / 100f,
                borderAlpha = s.glass.border / 100f,
                cornerRadiusDp = s.glass.cornerRadius.toFloat()
            )
        )
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
