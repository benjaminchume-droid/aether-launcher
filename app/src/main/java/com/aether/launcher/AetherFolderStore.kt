package com.aether.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AetherFolder(val id:String,val name:String,val packages:List<String>)

class AetherFolderStore(context:Context){
    private val prefs=context.applicationContext.getSharedPreferences("aether_folders",Context.MODE_PRIVATE)
    fun load():List<AetherFolder>=runCatching{
        val array=JSONArray(prefs.getString("folders","[]")? : "[]")
        val out=mutableListOf<AetherFolder>()
        for(i in 0 until array.length()){
            val o=array.getJSONObject(i); val a=o.optJSONArray("packages")?:JSONArray(); val pkgs=mutableListOf<String>()
            for(j in 0 until a.length()) pkgs.add(a.getString(j))
            out.add(AetherFolder(o.optString("id"),o.optString("name","Folder"),pkgs))
        }
        out
    }.getOrDefault(emptyList())
    @Synchronized fun save(folder:AetherFolder){write((load().filterNot{it.id==folder.id}+folder.copy(packages=folder.packages.distinct())).toList())}
    @Synchronized fun delete(id:String){write(load().filterNot{it.id==id})}
    private fun write(folders:List<AetherFolder>){val a=JSONArray();folders.forEach{f->a.put(JSONObject().apply{put("id",f.id);put("name",f.name);put("packages",JSONArray(f.packages))})};prefs.edit().putString("folders",a.toString()).apply()}
}
