package com.floatify.app.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "floatify_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SELECTED_APPS = "selected_apps"
        private const val KEY_BUBBLE_ACTIVE = "bubble_active"
        private const val KEY_NOTIFICATION_LISTENER_ENABLED = "notification_listener_enabled"
        private const val KEY_MONITORED_APPS = "monitored_apps"
        private const val KEY_SHOW_NOTIFICATIONS_IN_BUBBLE = "show_notifications_in_bubble"
        
        const val THEME_AUTO = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    
    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }
    
    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, THEME_AUTO)
    
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }
    
    fun getSelectedApps(): Set<String> = 
        prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
    
    fun setSelectedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_SELECTED_APPS, apps).apply()
    }
    
    fun addSelectedApp(packageName: String) {
        val apps = getSelectedApps().toMutableSet()
        apps.add(packageName)
        setSelectedApps(apps)
    }
    
    fun removeSelectedApp(packageName: String) {
        val apps = getSelectedApps().toMutableSet()
        apps.remove(packageName)
        setSelectedApps(apps)
    }
    
    fun isBubbleActive(): Boolean = prefs.getBoolean(KEY_BUBBLE_ACTIVE, false)
    
    fun setBubbleActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_BUBBLE_ACTIVE, active).apply()
    }
    
    fun isNotificationListenerEnabled(): Boolean = 
        prefs.getBoolean(KEY_NOTIFICATION_LISTENER_ENABLED, false)
    
    fun setNotificationListenerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_LISTENER_ENABLED, enabled).apply()
    }
    
    fun getMonitoredApps(): Set<String> = 
        prefs.getStringSet(KEY_MONITORED_APPS, emptySet()) ?: emptySet()
    
    fun setMonitoredApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_MONITORED_APPS, apps).apply()
    }
    
    fun addMonitoredApp(packageName: String) {
        val apps = getMonitoredApps().toMutableSet()
        apps.add(packageName)
        setMonitoredApps(apps)
    }
    
    fun removeMonitoredApp(packageName: String) {
        val apps = getMonitoredApps().toMutableSet()
        apps.remove(packageName)
        setMonitoredApps(apps)
    }
    
    fun isShowNotificationsInBubble(): Boolean = 
        prefs.getBoolean(KEY_SHOW_NOTIFICATIONS_IN_BUBBLE, true)
    
    fun setShowNotificationsInBubble(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTIFICATIONS_IN_BUBBLE, show).apply()
    }
}
