package com.example.abro.data.repositories

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
    
    fun isNightModeEnabled(): Boolean {
        return prefs.getBoolean("night_mode", false)
    }
    
    fun setNightModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("night_mode", enabled).apply()
    }
    
    fun clearBrowserData(context: Context) {
        // Clear browser preferences
        val browserPrefs = context.getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        browserPrefs.edit().clear().apply()
        
        // Clear bookmarks
        val bookmarkPrefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        bookmarkPrefs.edit().clear().apply()
    }
}