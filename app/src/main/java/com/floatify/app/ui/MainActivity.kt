package com.floatify.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import com.floatify.app.FloatifyApp
import com.floatify.app.R
import com.floatify.app.data.AppInfo
import com.floatify.app.databinding.ActivityMainBinding
import com.floatify.app.service.FloatBubbleService
import com.floatify.app.util.PrefsManager

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var appAdapter: AppListAdapter
    private var isBubbleActive = false
    private var installedApps: List<AppInfo> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        requestNotificationPermissionIfNeeded()
        
        try {
            setupAppList()
            setupThemeSpinner()
            setupActivateButton()
            updateStatus()
            updateAppCount()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun setupAppList() {
        installedApps = getInstalledApps()
        val selectedApps = prefs.getSelectedApps()
        
        installedApps.forEach { app ->
            app.isSelected = selectedApps.contains(app.packageName)
        }
        
        appAdapter = AppListAdapter(installedApps) { app, isSelected ->
            if (isSelected) {
                prefs.addSelectedApp(app.packageName)
            } else {
                prefs.removeSelectedApp(app.packageName)
            }
            updateBubbleService()
            updateAppCount()
        }
        
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
            setHasFixedSize(false)
        }
    }
    
    private fun getInstalledApps(): List<AppInfo> {
        return try {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val apps = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .mapNotNull { resolveInfo ->
                    try {
                        AppInfo(
                            name = resolveInfo.loadLabel(pm).toString(),
                            packageName = resolveInfo.activityInfo.packageName,
                            icon = resolveInfo.loadIcon(pm)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load app info: ${e.message}")
                        null
                    }
                }
                .sortedBy { it.name.lowercase() }
            
            Log.d(TAG, "Loaded ${apps.size} installed apps")
            apps
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun updateAppCount() {
        val selectedCount = prefs.getSelectedApps().size
        val totalCount = installedApps.size
        
        binding.tvAppCount.text = if (selectedCount > 0) {
            getString(R.string.app_count_format, selectedCount)
        } else {
            getString(R.string.all_apps_available, totalCount)
        }
    }
    
    private fun setupThemeSpinner() {
        val themes = arrayOf(
            getString(R.string.theme_auto),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTheme.adapter = adapter
        binding.spTheme.setSelection(prefs.getThemeMode())
        
        binding.spTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.setThemeMode(position)
                when (position) {
                    PrefsManager.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    PrefsManager.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupActivateButton() {
        binding.btnActivate.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, getString(R.string.error_no_permission), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            if (isBubbleActive) {
                stopBubbleService()
            } else {
                startBubbleService()
            }
        }
    }
    
    private fun startBubbleService() {
        try {
            (application as? FloatifyApp)?.ensureNotificationChannelExists()
            
            val intent = Intent(this, FloatBubbleService::class.java)
            ContextCompat.startForegroundService(this, intent)
            isBubbleActive = true
            prefs.setBubbleActive(true)
            updateStatus()
            Log.d(TAG, "Bubble service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting bubble service: ${e.message}", e)
            Toast.makeText(this, getString(R.string.error_service_failed), Toast.LENGTH_SHORT).show()
            isBubbleActive = false
            prefs.setBubbleActive(false)
            updateStatus()
        }
    }
    
    private fun stopBubbleService() {
        try {
            val intent = Intent(this, FloatBubbleService::class.java)
            stopService(intent)
            Log.d(TAG, "Bubble service stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping bubble service: ${e.message}", e)
        } finally {
            isBubbleActive = false
            prefs.setBubbleActive(false)
            updateStatus()
        }
    }
    
    private fun updateBubbleService() {
        if (isBubbleActive) {
            try {
                val intent = Intent(this, FloatBubbleService::class.java).apply {
                    action = FloatBubbleService.ACTION_UPDATE_APPS
                }
                startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating bubble service: ${e.message}", e)
            }
        }
    }
    
    private fun updateStatus() {
        isBubbleActive = prefs.isBubbleActive() && FloatBubbleService.isRunning
        
        binding.tvStatus.text = if (isBubbleActive) 
            getString(R.string.bubble_active) 
        else 
            getString(R.string.bubble_inactive)
            
        binding.tvStatus.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (isBubbleActive) R.color.success else R.color.primary_light
        )
        
        binding.btnActivate.text = if (isBubbleActive) 
            getString(R.string.deactivate_bubble) 
        else 
            getString(R.string.activate_bubble)
            
        binding.btnActivate.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (isBubbleActive) R.color.error else R.color.primary
        )
    }
}
