package com.floatify.app.service

import android.app.Notification
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.floatify.app.data.NotificationInfo
import com.floatify.app.util.PrefsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class FloatifyNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "FloatifyNotifListener"
        const val ACTION_NOTIFICATION_POSTED = "com.floatify.app.NOTIFICATION_POSTED"
        const val ACTION_NOTIFICATION_REMOVED = "com.floatify.app.NOTIFICATION_REMOVED"
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        
        @Volatile
        var isListenerConnected = false
            private set
        
        private val notifications = CopyOnWriteArrayList<NotificationInfo>()
        private const val MAX_NOTIFICATIONS = 50
        
        fun getNotifications(): List<NotificationInfo> {
            return notifications.toList()
        }
        
        fun getNotificationCount(): Int {
            return notifications.size
        }
        
        fun clearNotifications() {
            notifications.clear()
        }
        
        fun removeNotification(key: String) {
            notifications.removeAll { it.key == key }
        }
    }
    
    private var prefs: PrefsManager? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            prefs = PrefsManager(this)
            Log.d(TAG, "NotificationListener service created")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        Log.d(TAG, "NotificationListener connected")
        
        try {
            loadActiveNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading active notifications: ${e.message}", e)
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        Log.d(TAG, "NotificationListener disconnected")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn == null) return
        
        try {
            if (!shouldProcessNotification(sbn)) {
                return
            }
            
            val notificationInfo = extractNotificationInfo(sbn)
            if (notificationInfo != null) {
                notifications.removeAll { it.key == notificationInfo.key }
                notifications.add(0, notificationInfo)
                
                while (notifications.size > MAX_NOTIFICATIONS) {
                    notifications.removeAt(notifications.size - 1)
                }
                
                broadcastNotificationChange(ACTION_NOTIFICATION_POSTED, sbn.key)
                
                Log.d(TAG, "Notification posted from: ${sbn.packageName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}", e)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        
        if (sbn == null) return
        
        try {
            notifications.removeAll { it.key == sbn.key }
            broadcastNotificationChange(ACTION_NOTIFICATION_REMOVED, sbn.key)
            Log.d(TAG, "Notification removed from: ${sbn.packageName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing notification: ${e.message}", e)
        }
    }
    
    private fun shouldProcessNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) {
            return false
        }
        
        if (!sbn.isClearable) {
            return false
        }
        
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) {
            return false
        }
        
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return false
        }
        
        val monitoredApps = prefs?.getMonitoredApps() ?: emptySet()
        if (monitoredApps.isNotEmpty() && !monitoredApps.contains(sbn.packageName)) {
            return false
        }
        
        return true
    }
    
    private fun extractNotificationInfo(sbn: StatusBarNotification): NotificationInfo? {
        return try {
            val notification = sbn.notification
            val extras = notification.extras
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            if (title.isEmpty() && text.isEmpty()) {
                return null
            }
            
            val pm = packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                sbn.packageName
            }
            
            val appIcon: Drawable? = try {
                pm.getApplicationIcon(sbn.packageName)
            } catch (e: Exception) {
                null
            }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = timeFormat.format(Date(sbn.postTime))
            
            NotificationInfo(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                appIcon = appIcon,
                title = title,
                text = bigText ?: text,
                postTime = sbn.postTime,
                timeString = timeString
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting notification info: ${e.message}", e)
            null
        }
    }
    
    private fun loadActiveNotifications() {
        try {
            val activeNotifications = activeNotifications ?: return
            
            notifications.clear()
            
            activeNotifications
                .filter { shouldProcessNotification(it) }
                .sortedByDescending { it.postTime }
                .take(MAX_NOTIFICATIONS)
                .mapNotNull { extractNotificationInfo(it) }
                .forEach { notifications.add(it) }
            
            Log.d(TAG, "Loaded ${notifications.size} active notifications")
            
            if (notifications.isNotEmpty()) {
                broadcastNotificationChange(ACTION_NOTIFICATION_POSTED, "bulk_load")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading active notifications: ${e.message}", e)
        }
    }
    
    private fun broadcastNotificationChange(action: String, key: String) {
        try {
            val intent = Intent(action).apply {
                putExtra(EXTRA_NOTIFICATION_KEY, key)
                setPackage(packageName)
            }
            sendBroadcast(intent)
            
            val bubbleIntent = Intent(FloatBubbleService.ACTION_NOTIFICATION_UPDATE).apply {
                setPackage(packageName)
            }
            sendBroadcast(bubbleIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting notification change: ${e.message}", e)
        }
    }
    
    fun dismissNotification(key: String) {
        try {
            cancelNotification(key)
            notifications.removeAll { it.key == key }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        isListenerConnected = false
        super.onDestroy()
        Log.d(TAG, "NotificationListener service destroyed")
    }
}
