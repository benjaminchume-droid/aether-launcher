package com.aether.launcher.settings

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.aether.launcher.AetherRuntime
import com.aether.launcher.ui.GlassPainter

/**
 * Full first-run Setup Wizard.
 * Matches the reference: iOS-style Hello → progressive configuration,
 * wallpaper under glass with refraction on every page.
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
        alpha = 0.78f
        setImageDrawable(
            runCatching { WallpaperManager.getInstance(context).drawable }
                .getOrElse { ColorDrawable(0xFF10141A.toInt()) }
        )
    }

    private val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(36), dp(24), dp(36))
    }

    init {
        addView(wallpaper, LayoutParams(-1, -1))
        // Soft atmosphere so glass pops
        addView(View(context).apply { setBackgroundColor(0x4403070D) }, LayoutParams(-1, -1))
        addView(ScrollView(context).apply {
            isFillViewport = true
            addView(body)
        }, LayoutParams(-1, -1))
        render()
    }

    private fun render() {
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
                text = "‹  Back"
                textSize = 15f
                setTextColor(0xCFFFFFFF.toInt())
                gravity = Gravity.CENTER
                setOnClickListener { page--; render() }
            }, lp(-1, 48))
        }
    }

    private fun progress() {
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER }
        for (i in 0..5) {
            val v = View(context).apply {
                setBackgroundColor(if (i == page) 0xEFFFFFFF.toInt() else 0x55FFFFFF)
                alpha = if (i == page) 1f else 0.7f
            }
            row.addView(v, LinearLayout.LayoutParams(
                dp(if (i == page) 36 else 14), dp(3)
            ).also { it.setMargins(dp(3), 0, dp(3), 0) })
        }
        body.addView(row, 0, lp(-1, 28))
    }

    // ─── Page 0: Hello (reference style) ───
    private fun hello() {
        body.addView(View(context), lp(-1, 80))
        body.addView(TextView(context).apply {
            text = "hello"
            textSize = 54f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(24f, 0f, 6f, 0x80000000)
        }, lp(-1, 90))
        body.addView(TextView(context).apply {
            text = "Welcome to Aether"
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
        }, lp(-1, 36))
        body.addView(View(context), lp(-1, 40))
        body.addView(TextView(context).apply {
            text = "A calm, fluid workspace built around your wallpaper,\nyour apps, and the way you move between them."
            textSize = 14f
            setTextColor(0xB8FFFFFF.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
        }, lp(-1, 70))
        body.addView(View(context), lp(-1, 30))
        body.addView(TextView(context).apply {
            text = "Swipe up or tap Continue"
            textSize = 13f
            setTextColor(0x99FFFFFF.toInt())
            gravity = Gravity.CENTER
        }, lp(-1, 40))
    }

    // ─── Page 1: Home mode ───
    private fun homeChoice() {
        title("How should Home behave?")
        paragraph("This is the only choice that changes your first interaction. Everything stays editable later.")
        option("All apps on Home", "Every installed launcher app lives on paged Home.", HomeMode.ALL_APPS)
        option("Home + App Drawer", "Keep Home clean and open the full library from the drawer.", HomeMode.HOME_AND_DRAWER)
        option("App Drawer only", "Minimal Home with the library as the primary surface.", HomeMode.DRAWER_ONLY)
    }

    // ─── Page 2: Grid + look ───
    private fun gridAndLook() {
        title("Grid & look")
        paragraph("4×4 through 9×9. Icon size, labels, dock and glass depth are all adjustable later.")
        glassPanel("GRID", "${s.grid.columns}×${s.grid.rows}  •  icon ${s.grid.iconSize}dp  •  labels ${if (s.grid.showLabels) "on" else "off"}")
        glassPanel("DOCK", if (s.dock.enabled) "Enabled  •  ${s.dock.appCount} apps" else "Disabled")
        glassPanel("GLASS", "Opacity ${s.glass.opacity}%  •  blur ${s.glass.blur}  •  depth ${s.glass.depth}")
        // Simple steppers
        stepper("Columns", s.grid.columns, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(columns = v))
            store.save(s); render()
        }
        stepper("Rows", s.grid.rows, 4, 9) { v ->
            s = s.copy(grid = s.grid.copy(rows = v))
            store.save(s); render()
        }
    }

    // ─── Page 3: Island + Quick Space ───
    private fun islandAndQuick() {
        title("Island & Quick Space")
        paragraph("The capsule lives over every app. Quick Space lives on the edge.")
        toggleRow("Dynamic Island", s.island.enabled) {
            s = s.copy(island = s.island.copy(enabled = it))
            store.save(s); render()
        }
        toggleRow("Cutout aware", s.island.cutoutAware) {
            s = s.copy(island = s.island.copy(cutoutAware = it))
            store.save(s); render()
        }
        toggleRow("Quick Space", s.quickSpace.enabled) {
            s = s.copy(quickSpace = s.quickSpace.copy(enabled = it))
            store.save(s); render()
        }
        glassPanel("ISLAND", "Media • Calls • Downloads • Recording • Notifications • Reply")
        glassPanel("QUICK SPACE", "Notes • Recorder • Calculator • Screenshot • Clipboard • Recents")
    }

    // ─── Page 4: Permissions / system layer ───
    private fun systemLayer() {
        title("Unlock the Aether layer")
        paragraph("These are optional Android capabilities. Enable only what you want.")
        permission("Default Home", "Let Aether become the real Android Home role.") {
            runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        permission("Liquid Island", "Allow Aether to render its floating system layer.") {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
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

    // ─── Page 5: Finish ───
    private fun finishPage() {
        title("You're ready.")
        paragraph("Aether keeps your layout, folders, dock and security choices on-device. Change anything later from Aether Settings.")
        glassPanel(
            "STARTING SETUP",
            "${s.homeMode.name.replace('_', ' ')}  •  ${AetherRuntime.registry.launcher.apps().size} apps  •  Glass on"
        )
        glassPanel(
            "GESTURES",
            "Swipe pages  •  swipe down for Search  •  edge for Quick Space  •  long-press + drag to merge"
        )
        glassPanel(
            "SYSTEM LAYER",
            "Island + Quick Space run over every app once overlay permission is granted"
        )
    }

    // ─── Helpers ───

    private fun title(text: String) {
        body.addView(TextView(context).apply {
            this.text = text
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setShadowLayer(16f, 0f, 4f, 0x70000000)
        }, lp(-1, 70))
    }

    private fun paragraph(text: String) {
        body.addView(TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xD0FFFFFF.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1f)
            setPadding(dp(8), 0, dp(8), 0)
        }, lp(-1, 70))
    }

    private fun glassPanel(head: String, detail: String) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = glassDrawable(0x6F111820.toInt(), 24f)
            elevation = dp(6).toFloat()
        }
        box.addView(TextView(context).apply {
            text = head
            textSize = 11f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xF2FFFFFF.toInt())
        })
        box.addView(TextView(context).apply {
            text = detail
            textSize = 12f
            setTextColor(0xCFFFFFFF.toInt())
            setPadding(0, dp(5), 0, 0)
            setLineSpacing(3f, 1f)
        })
        body.addView(box, lp(-1, -2).also { it.setMargins(0, dp(6), 0, dp(6)) })
    }

    private fun option(head: String, detail: String, value: HomeMode) {
        val selected = s.homeMode == value
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = glassDrawable(if (selected) 0x9A5D8FE4.toInt() else 0x64111820.toInt(), 24f)
            setOnClickListener {
                s = s.copy(homeMode = value)
                store.save(s)
                render()
            }
        }
        box.addView(TextView(context).apply {
            text = (if (selected) "●  " else "○  ") + head
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        box.addView(TextView(context).apply {
            text = detail
            textSize = 11f
            setTextColor(0xCFFFFFFF.toInt())
            setPadding(dp(22), dp(4), 0, 0)
        })
        body.addView(box, lp(-1, 76).also { it.setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun toggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(12), dp(10))
            background = glassDrawable(0x68111820.toInt(), 22f)
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
            background = glassDrawable(0x68111820.toInt(), 22f)
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(context).apply {
            text = "−"
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
            background = glassDrawable(0x68111820.toInt(), 22f)
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
                setTextColor(0xBFFFFFFF.toInt())
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(TextView(context).apply {
            text = "OPEN"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xEFFFFFFF.toInt())
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
        background = glassDrawable(0xB65B8FEA.toInt(), 28f)
        elevation = dp(10).toFloat()
        setOnClickListener { onClick() }
    }

    private fun glassDrawable(base: Int, radius: Float) = GradientDrawable().apply {
        setColor(base)
        cornerRadius = dp(radius.toInt()).toFloat()
        setStroke(dp(1), 0x55FFFFFF)
    }

    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    private fun dp(v: Int) = (v * d).toInt()
}
