package com.aether.launcher.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/** Real AppWidgetHost: the system picker supplies provider-backed live widgets. */
class AetherWidgetHostActivity : Activity() {
    private lateinit var host: AppWidgetHost
    private lateinit var root: LinearLayout
    private val hostId = 0xAE7E
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 32) }
        val title = TextView(this).apply { text = "Aether Widgets"; textSize = 28f; setTextColor(0xFFFFFFFF.toInt()); setPadding(0,0,0,20) }
        val add = TextView(this).apply { text = "+ Add live widget"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt()); setPadding(20,20,20,20); setOnClickListener { pickWidget() } }
        root.addView(title); root.addView(add); setContentView(root)
        host = AppWidgetHost(this, hostId)
    }
    override fun onStart() { super.onStart(); host.startListening() }
    override fun onStop() { host.stopListening(); super.onStop() }
    private fun pickWidget() { val id = host.allocateAppWidgetId(); startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id), 41) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 41) return
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0) ?: 0
        if (resultCode != RESULT_OK || id == 0) { if (id != 0) host.deleteAppWidgetId(id); return }
        val manager = AppWidgetManager.getInstance(this)
        val info = manager.getAppWidgetInfo(id) ?: run { host.deleteAppWidgetId(id); return }
        if (info.configure != null) startActivityForResult(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setComponent(info.configure), 42)
        addWidget(manager, id)
    }
    private fun addWidget(manager: AppWidgetManager, id: Int) {
        val view: AppWidgetHostView = host.createView(this, id, manager.getAppWidgetInfo(id))
        root.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16 })
    }
}
