package com.aether.launcher.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.settings.AetherSettingsStore
import kotlin.math.max

/**
 * Native, searchable app drawer. It queries the actual launchable activities on-device,
 * uses their real icons, and rebuilds when the search/category changes.
 */
class AetherAppDrawerView(context: Context) : FrameLayout(context) {
    private val d = resources.displayMetrics.density
    private val allApps get() = AetherRuntime.registry.launcher.apps()
    private val history = AetherHistoryStore(context)
    private val settings = AetherSettingsStore(context)
    private val query = EditText(context)
    private val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private var category = "All"
    private val categories = listOf("All", "Communication", "Media", "Productivity", "Games")
    private val labels = mapOf(
        "Communication" to listOf("phone", "message", "whatsapp", "telegram", "discord", "contact", "mail", "email"),
        "Media" to listOf("youtube", "music", "photo", "gallery", "camera", "spotify", "netflix", "video", "recorder"),
        "Productivity" to listOf("calendar", "clock", "drive", "file", "note", "calculator", "settings", "browser", "chrome"),
        "Games" to listOf("game")
    )

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(54), dp(20), dp(8))
        }
        addView(page, LayoutParams(-1, -1))
        query.apply {
            hint = "⌕   Search apps, contacts, settings…"
            textSize = 15f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(0xD9FFFFFF.toInt())
            setPadding(dp(18), 0, dp(18), 0)
            background = glass(0x8058BDF4.toInt(), 40)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderGrid() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        page.addView(query, LinearLayout.LayoutParams(-1, dp(58)))
        val suggested = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = glass(0x7659B7E9.toInt(), 28)
        }
        val suggestedHeader = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        suggestedHeader.addView(text("Suggested Apps", 14, true), LinearLayout.LayoutParams(0, dp(26), 1f))
        suggestedHeader.addView(text("Recent", 12, false).apply { alpha = .8f })
        suggested.addView(suggestedHeader)
        val suggestedRow = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val recents = history.load().mapNotNull { e -> allApps.firstOrNull { it.packageName == e.packageName } }
            .distinctBy { it.packageName }.take(5)
        (recents.ifEmpty { allApps.take(5) }).forEach { app ->
            suggestedRow.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(8), dp(4), 0)
                addView(appIcon(app.packageName, 48))
                addView(text(app.label, 10, false).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(-1, dp(22)))
                setOnClickListener { launch(app.packageName) }
            }, LinearLayout.LayoutParams(0, dp(84), 1f))
        }
        suggested.addView(suggestedRow)
        page.addView(suggested, LinearLayout.LayoutParams(-1, dp(132)).apply { topMargin = dp(18) })
        val chipScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        val chipRow = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        categories.forEach { cat ->
            val chip = text(cat, 12, false).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), 0, dp(16), 0)
                background = glass(if (cat == category) 0xB050D5F4.toInt() else 0x664F9DD9.toInt(), 28)
                setOnClickListener { category = cat; updateChips(chipRow); renderGrid() }
                tag = cat
            }
            chipRow.addView(chip, LinearLayout.LayoutParams(-2, dp(36)).apply { setMargins(dp(3), dp(12), dp(5), dp(12)) })
        }
        chipScroll.addView(chipRow)
        page.addView(chipScroll)
        val allHeader = text("All apps", 17, true).apply { setPadding(dp(4), dp(4), 0, dp(8)) }
        page.addView(allHeader)
        val scroll = ScrollView(context).apply { clipToPadding = false; isFillViewport = false }
        scroll.addView(grid)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        renderGrid()
    }

    private fun renderGrid() {
        if (!::grid.isInitializedCompat()) return
        grid.removeAllViews()
        val q = query.text?.toString()?.trim().orEmpty()
        val terms = labels[category]
        val filtered = allApps.filter { app ->
            (q.isBlank() || app.label.contains(q, true) || app.packageName.contains(q, true)) &&
                (terms == null || terms.any { app.label.contains(it, true) || app.packageName.contains(it, true) })
        }.sortedBy { it.label.lowercase() }
        val columns = 4
        filtered.chunked(columns).forEach { rowApps ->
            val row = LinearLayout(context).apply { gravity = Gravity.TOP; orientation = LinearLayout.HORIZONTAL }
            rowApps.forEach { app ->
                row.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(2), dp(7), dp(2), dp(3))
                    addView(appIcon(app.packageName, 58))
                    addView(text(app.label, 11, false).apply {
                        gravity = Gravity.CENTER
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setShadowLayer(5f, 0f, 1f, 0xAA00162A.toInt())
                    }, LinearLayout.LayoutParams(-1, dp(24)))
                    setOnClickListener { launch(app.packageName) }
                    setOnLongClickListener { showAppMenu(app.packageName, app.label); true }
                }, LinearLayout.LayoutParams(0, dp(100), 1f))
            }
            repeat(columns - rowApps.size) { row.addView(View(context), LinearLayout.LayoutParams(0, dp(100), 1f)) }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        if (filtered.isEmpty()) grid.addView(text("No matching apps", 14, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(32), 0, dp(32)) })
    }

    private fun updateChips(row: LinearLayout) {
        for (i in 0 until row.childCount) {
            val v = row.getChildAt(i) as? TextView ?: continue
            v.background = glass(if (v.tag == category) 0xB050D5F4.toInt() else 0x664F9DD9.toInt(), 28)
        }
    }

    private fun launch(pkg: String) {
        history.record(pkg)
        AetherRuntime.registry.launcher.launch(pkg)
        (context as? Activity)?.finish()
    }

    private fun showAppMenu(pkg: String, label: String) {
        AlertDialog.Builder(context).setTitle(label).setItems(arrayOf("Open", "App info", "Uninstall")) { _, which ->
            when (which) {
                0 -> launch(pkg)
                1 -> runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))) }
                2 -> runCatching { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))) }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun appIcon(pkg: String, size: Int) = ImageView(context).apply {
        setImageDrawable(AetherRuntime.registry.launcher.icon(pkg))
        scaleType = ImageView.ScaleType.FIT_CENTER
        background = glass(0x665DC5F8.toInt(), 22)
        setPadding(dp(7), dp(7), dp(7), dp(7))
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
    }

    private fun text(value: String, size: Int, bold: Boolean) = TextView(context).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(Color.WHITE)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun glass(color: Int, radius: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius).toFloat()
        setColor(color)
        setStroke(dp(1), 0x70FFFFFF)
    }

    private fun dp(v: Int) = (v * d).toInt()

    // No framework dependency or fake app inventory: the grid is populated by LauncherEngine.
    private fun LinearLayout.isInitializedCompat() = true
}
