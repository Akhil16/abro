package com.example.abro.ui.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.abro.R
import com.example.abro.data.repositories.SettingsRepository

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var nightModeSwitch: Switch
    private lateinit var clearDataBtn: Button
    private lateinit var aboutBtn: Button
    private lateinit var backBtn: Button
    private lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        settingsRepository = SettingsRepository(this)
        
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
            settingsRepository.setNightModeEnabled(isChecked)
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
        nightModeSwitch.isChecked = settingsRepository.isNightModeEnabled()
    }
    
    private fun applyNightMode(isNightMode: Boolean) {
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun clearBrowserData() {
        settingsRepository.clearBrowserData(this)
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