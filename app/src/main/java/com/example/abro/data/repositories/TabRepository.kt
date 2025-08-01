package com.example.abro.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TabRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveTabUrls(urls: List<String>) {
        val json = gson.toJson(urls)
        prefs.edit().putString("tabs_json", json).apply()
    }
    
    fun loadTabUrls(): List<String> {
        val json = prefs.getString("tabs_json", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}