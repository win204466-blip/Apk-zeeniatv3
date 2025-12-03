package com.floatify.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.floatify.app.util.PrefsManager

class FloatifyApp : Application() {
    
    companion object {
        private const val TAG = "FloatifyApp"
        const val NOTIFICATION_CHANNEL_ID = "floatify_service"
    }
    
    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
        try {
            ensureNotificationChannelExists()
            applyTheme()
            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during application initialization: ${e.message}", e)
        }
    }
    
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    fun ensureNotificationChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = getSystemService(NotificationManager::class.java)
                if (manager == null) {
                    Log.e(TAG, "NotificationManager is null")
                    return
                }
                
                val existingChannel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
                if (existingChannel != null) {
                    Log.d(TAG, "Notification channel already exists")
                    return
                }
                
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel: ${e.message}", e)
            }
        }
    }
    
    private fun applyTheme() {
        try {
            val prefs = PrefsManager(this)
            when (prefs.getThemeMode()) {
                PrefsManager.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                PrefsManager.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            Log.d(TAG, "Theme applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme: ${e.message}", e)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
