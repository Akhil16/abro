package com.example.abro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var nightModeSwitch: Switch
    private lateinit var clearDataBtn: Button
    private lateinit var aboutBtn: Button
    private lateinit var backBtn: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initViews()
        setupListeners()
        loadSettings()
    }
    
    private fun initViews() {
        nightModeSwitch = findViewById(R.id.night_mode_switch)
        clearDataBtn = findViewById(R.id.clear_data_btn)
        aboutBtn = findViewById(R.id.about_btn)
        backBtn = findViewById(R.id.back_btn)
    }
    
    private fun setupListeners() {
        backBtn.setOnClickListener { finish() }
        
        nightModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNightModeSetting(isChecked)
            applyNightMode(isChecked)
        }
        
        clearDataBtn.setOnClickListener {
            clearBrowserData()
        }
        
        aboutBtn.setOnClickListener {
            showAboutDialog()
        }
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
        val isNightMode = prefs.getBoolean("night_mode", false)
        nightModeSwitch.isChecked = isNightMode
    }
    
    private fun saveNightModeSetting(isNightMode: Boolean) {
        val prefs = getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("night_mode", isNightMode).apply()
    }
    
    private fun applyNightMode(isNightMode: Boolean) {
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun clearBrowserData() {
        val prefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        val bookmarkPrefs = getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        bookmarkPrefs.edit().clear().apply()
        
        Toast.makeText(this, "Browser data cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAboutDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("About Abro Browser")
        builder.setMessage("Abro Browser v1.0\n\nA simple and fast web browser for Android.")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}