package com.aether.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.aether.launcher.AetherHistoryStore
import com.aether.launcher.AetherRuntime
import com.aether.launcher.engine.launcher.AppInfo

class AetherSearchActivity : Activity() {
    private val allApps by lazy { AetherRuntime.registry.launcher.apps() }
    private lateinit var results: LinearLayout
    private lateinit var empty: TextView
    private val history by lazy { AetherHistoryStore(this) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        AetherRuntime.initialize(applicationContext)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(20))
            setBackgroundColor(0xFF080A0E.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Search Aether"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(-1, dp(48)))
        val query = EditText(this).apply {
            hint = "Search installed apps…"
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(0x88FFFFFF.toInt())
            setBackgroundColor(0x221FFFFFFF.toInt())
            setPadding(dp(16), 0, dp(16), 0)
            requestFocus()
        }
        root.addView(query, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(14) })
        val scroll = ScrollView(this)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(results)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        empty = TextView(this).apply {
            text = "Recent apps and installed apps"
            textSize = 14f
            setTextColor(0xBFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(empty, LinearLayout.LayoutParams(-1, dp(74)))
        setContentView(root)
        query.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        render("")
    }

    private fun render(raw: String) {
        results.removeAllViews()
        val q = raw.trim().lowercase()
        val matches = if (q.isBlank()) allApps.take(12) else allApps.asSequence()
            .filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            .take(30).toList()
        matches.forEach(::addResult)
        empty.visibility = if (matches.isEmpty()) TextView.VISIBLE else TextView.GONE
        if (q.isBlank() && matches.isNotEmpty()) empty.text = "Recent apps and installed apps"
        if (q.isNotBlank() && matches.isEmpty()) empty.text = "No installed app matches \"$raw\""
    }

    private fun addResult(app: AppInfo) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(0x141FFFFFFF)
            setOnClickListener {
                history.record(app.packageName)
                AetherRuntime.registry.launcher.launchIntent(app.packageName)?.let(::startActivity)
                finish()
            }
        }
        row.addView(ImageView(this).apply {
            setImageDrawable(AetherRuntime.registry.launcher.icon(app.packageName))
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }, LinearLayout.LayoutParams(dp(54), dp(62)))
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        labels.addView(TextView(this).apply { text = app.label; textSize = 15f; setTextColor(Color.WHITE) })
        labels.addView(TextView(this).apply { text = app.packageName; textSize = 10f; setTextColor(0x7FFFFFFF.toInt()) })
        row.addView(labels, LinearLayout.LayoutParams(0, dp(62), 1f))
        results.addView(row, LinearLayout.LayoutParams(-1, dp(66)).apply { bottomMargin = dp(6) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
