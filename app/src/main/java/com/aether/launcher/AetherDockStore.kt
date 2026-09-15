package com.aether.launcher

import android.content.Context
import org.json.JSONArray

class AetherDockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_dock", Context.MODE_PRIVATE)

    fun favorites(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("favorites", "[]") ?: "[]")
        buildList(array.length()) { for (i in 0 until array.length()) add(array.getString(i)) }
    }.getOrDefault(emptyList())

    @Synchronized fun toggle(packageName: String) {
        if (packageName.isBlank()) return
        val current = favorites().toMutableList()
        if (!current.remove(packageName)) current.add(packageName)
        prefs.edit().putString("favorites", JSONArray(current).toString()).apply()
    }

    @Synchronized fun set(packages: List<String>) {
        prefs.edit().putString("favorites", JSONArray(packages.distinct()).toString()).apply()
    }
}
