package com.aether.launcher

import android.content.Context
import org.json.JSONArray

data class AetherHistoryEntry(val packageName: String, val timestamp: Long)

class AetherHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_history", Context.MODE_PRIVATE)
    @Synchronized fun record(packageName: String) {
        if (packageName.isBlank() || packageName == contextPackage) return
        val current = load().filterNot { it.packageName == packageName }.toMutableList()
        current.add(0, AetherHistoryEntry(packageName, System.currentTimeMillis()))
        val json = JSONArray().apply { current.take(50).forEach { put(JSONArray().apply { put(it.packageName); put(it.timestamp) }) } }
        prefs.edit().putString("entries", json.toString()).apply()
    }
    fun load(): List<AetherHistoryEntry> = runCatching {
        val arr = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        buildList(arr.length()) { for (i in 0 until arr.length()) { val row = arr.getJSONArray(i); add(AetherHistoryEntry(row.getString(0), row.getLong(1))) } }
    }.getOrDefault(emptyList())
    @Synchronized fun clear() = prefs.edit().remove("entries").apply()
    private val contextPackage get() = prefs.getString("contextPackage", null).also { if (it == null) prefs.edit().putString("contextPackage", context.packageName).apply() } ?: context.packageName
}
