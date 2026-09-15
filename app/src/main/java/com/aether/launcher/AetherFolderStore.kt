package com.aether.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AetherFolder(val id: String, val name: String, val packages: List<String>)

class AetherFolderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_folders", Context.MODE_PRIVATE)

    fun load(): List<AetherFolder> = runCatching {
        val array = JSONArray(prefs.getString("folders", "[]") ?: "[]")
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val apps = o.optJSONArray("packages") ?: JSONArray()
                add(AetherFolder(o.optString("id"), o.optString("name", "Folder"), buildList(apps.length()) { j -> add(apps.getString(j)) }))
            }
        }
    }.getOrDefault(emptyList())

    @Synchronized fun save(folder: AetherFolder) {
        val folders = load().filterNot { it.id == folder.id } + folder.copy(packages = folder.packages.distinct())
        val array = JSONArray()
        folders.forEach { f ->
            array.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("packages", JSONArray(f.packages))
            })
        }
        prefs.edit().putString("folders", array.toString()).apply()
    }

    @Synchronized fun delete(id: String) {
        val folders = load().filterNot { it.id == id }
        val array = JSONArray()
        folders.forEach { f ->
            array.put(JSONObject().apply { put("id", f.id); put("name", f.name); put("packages", JSONArray(f.packages)) })
        }
        prefs.edit().putString("folders", array.toString()).apply()
    }
}
