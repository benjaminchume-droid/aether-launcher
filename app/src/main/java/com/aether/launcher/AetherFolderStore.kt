package com.aether.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AetherFolder(val id: String, val name: String, val packages: List<String>)

class AetherFolderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aether_folders", Context.MODE_PRIVATE)

    fun load(): List<AetherFolder> {
        return runCatching {
            val array = JSONArray(prefs.getString("folders", "[]") ?: "[]")
            val out = mutableListOf<AetherFolder>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val packageArray = o.optJSONArray("packages") ?: JSONArray()
                val packages = mutableListOf<String>()
                for (j in 0 until packageArray.length()) {
                    packages.add(packageArray.getString(j))
                }
                out.add(AetherFolder(o.optString("id"), o.optString("name", "Folder"), packages.distinct()))
            }
            out
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(folder: AetherFolder) {
        val folders = load().filterNot { it.id == folder.id }.toMutableList()
        folders.add(folder.copy(packages = folder.packages.distinct()))
        write(folders)
    }

    @Synchronized
    fun delete(id: String) {
        write(load().filterNot { it.id == id })
    }

    private fun write(folders: List<AetherFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("packages", JSONArray(folder.packages))
            })
        }
        prefs.edit().putString("folders", array.toString()).apply()
    }
}
