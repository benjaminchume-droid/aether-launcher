package com.aether.launcher

import android.content.Context
import org.json.JSONArray

/** Persists the exact home ordering. A folder is represented by folder:<id>. */
class AetherHomeLayoutStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_home_layout", Context.MODE_PRIVATE)

    fun order(installed: List<String>): MutableList<String> {
        val saved = runCatching {
            val a = JSONArray(prefs.getString("order", "[]") ?: "[]")
            buildList(a.length()) { for (i in 0 until a.length()) add(a.getString(i)) }
        }.getOrDefault(emptyList())
        val validPackages = installed.toSet()
        val result = saved.filter { it.startsWith("folder:") || it in validPackages }.toMutableList()
        installed.forEach { pkg -> if (pkg !in result) result.add(pkg) }
        return result
    }

    @Synchronized fun save(order: List<String>) {
        prefs.edit().putString("order", JSONArray(order.distinct()).toString()).apply()
    }
}
