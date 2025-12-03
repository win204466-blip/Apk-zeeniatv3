package com.floatify.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.floatify.app.FloatifyApp
import com.floatify.app.R
import com.floatify.app.data.AppInfo
import com.floatify.app.data.NotificationInfo
import com.floatify.app.ui.MainActivity
import com.floatify.app.util.PrefsManager
import com.google.android.material.tabs.TabLayout

class FloatBubbleService : Service() {
    
    companion object {
        private const val TAG = "FloatBubbleService"
        const val ACTION_UPDATE_APPS = "com.floatify.app.UPDATE_APPS"
        const val ACTION_NOTIFICATION_UPDATE = "com.floatify.app.NOTIFICATION_UPDATE"
        private const val NOTIFICATION_ID = 1001
        
        @Volatile
        var isRunning = false
            private set
    }
    
    private var windowManager: WindowManager? = null
    private var prefs: PrefsManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var bubbleView: View? = null
    private var menuView: View? = null
    private var isMenuVisible = false
    private var bubbleParams: WindowManager.LayoutParams? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTouchTime = 0L
    
    private var notificationAdapter: NotificationAdapter? = null
    private var currentTab = 0
    
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_NOTIFICATION_UPDATE,
                FloatifyNotificationListener.ACTION_NOTIFICATION_POSTED,
                FloatifyNotificationListener.ACTION_NOTIFICATION_REMOVED -> {
                    mainHandler.post { safeUpdateNotifications() }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
            prefs = PrefsManager(this)
            isRunning = true
            registerNotificationReceiver()
            Log.d(TAG, "Service created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            stopSelf()
        }
    }
    
    private fun registerNotificationReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_NOTIFICATION_UPDATE)
                addAction(FloatifyNotificationListener.ACTION_NOTIFICATION_POSTED)
                addAction(FloatifyNotificationListener.ACTION_NOTIFICATION_REMOVED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(notificationReceiver, filter)
            }
            Log.d(TAG, "Notification receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering notification receiver: ${e.message}", e)
        }
    }
    
    private fun unregisterNotificationReceiver() {
        try {
            unregisterReceiver(notificationReceiver)
            Log.d(TAG, "Notification receiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering notification receiver: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_UPDATE_APPS -> {
                    mainHandler.post { safeUpdateMenuApps() }
                }
                else -> {
                    if (!hasOverlayPermission()) {
                        Log.w(TAG, "No overlay permission, stopping service")
                        prefs?.setBubbleActive(false)
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    
                    val notification = createNotification()
                    startForeground(NOTIFICATION_ID, notification)
                    mainHandler.post { safeShowBubble() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
            prefs?.setBubbleActive(false)
            return START_NOT_STICKY
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy called")
        isRunning = false
        prefs?.setBubbleActive(false)
        mainHandler.removeCallbacksAndMessages(null)
        unregisterNotificationReceiver()
        cleanupViews()
        super.onDestroy()
    }
    
    private fun cleanupViews() {
        try {
            val wm = windowManager
            
            bubbleView?.let { view ->
                try {
                    if (view.isAttachedToWindow && wm != null) {
                        wm.removeView(view)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing bubble view: ${e.message}")
                }
                Unit
            }
            bubbleView = null
            bubbleParams = null
            
            menuView?.let { view ->
                try {
                    if (view.isAttachedToWindow && wm != null) {
                        wm.removeView(view)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing menu view: ${e.message}")
                }
                Unit
            }
            menuView = null
            isMenuVisible = false
            
            Log.d(TAG, "Views cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during views cleanup: ${e.message}", e)
        }
    }
    
    private fun hasOverlayPermission(): Boolean {
        return try {
            android.provider.Settings.canDrawOverlays(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overlay permission: ${e.message}", e)
            false
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingIntentFlags
        )
        
        return NotificationCompat.Builder(this, FloatifyApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun safeShowBubble() {
        if (bubbleView != null) {
            Log.d(TAG, "Bubble already showing")
            return
        }
        
        if (!hasOverlayPermission()) {
            Log.w(TAG, "Cannot show bubble - no overlay permission")
            stopSelf()
            return
        }
        
        val wm = windowManager ?: run {
            Log.e(TAG, "WindowManager is null")
            stopSelf()
            return
        }
        
        try {
            bubbleView = LayoutInflater.from(this).inflate(R.layout.layout_bubble, null)
            
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            
            bubbleParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 50
                y = 300
            }
            
            safeUpdateBadgeCount()
            
            bubbleView?.setOnTouchListener { _, event ->
                handleBubbleTouch(event)
            }
            
            wm.addView(bubbleView, bubbleParams)
            Log.d(TAG, "Bubble shown successfully")
            
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException showing bubble: ${e.message}", e)
            bubbleView = null
            bubbleParams = null
            stopSelf()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException showing bubble: ${e.message}", e)
            bubbleView = null
            bubbleParams = null
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing bubble: ${e.message}", e)
            bubbleView = null
            bubbleParams = null
            stopSelf()
        }
    }
    
    private fun handleBubbleTouch(event: MotionEvent): Boolean {
        val params = bubbleParams ?: return false
        val bubble = bubbleView ?: return false
        
        return try {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastTouchTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    safeUpdateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                    val deltaY = kotlin.math.abs(event.rawY - initialTouchY)
                    val touchDuration = System.currentTimeMillis() - lastTouchTime
                    
                    if (deltaX < 15 && deltaY < 15 && touchDuration < 300) {
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling touch: ${e.message}", e)
            false
        }
    }
    
    private fun safeUpdateViewLayout(view: View, params: WindowManager.LayoutParams) {
        try {
            val wm = windowManager ?: return
            if (view.isAttachedToWindow) {
                wm.updateViewLayout(view, params)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached, cannot update layout")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating view layout: ${e.message}", e)
        }
    }
    
    private fun safeRemoveBubble() {
        val view = bubbleView ?: return
        val wm = windowManager ?: return
        
        try {
            if (view.isAttachedToWindow) {
                wm.removeView(view)
                Log.d(TAG, "Bubble removed successfully")
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Bubble view not attached")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing bubble: ${e.message}", e)
        } finally {
            bubbleView = null
            bubbleParams = null
        }
    }
    
    private fun toggleMenu() {
        if (isMenuVisible) {
            safeHideMenu()
        } else {
            safeShowMenu()
        }
    }
    
    private fun safeShowMenu() {
        if (menuView != null) {
            Log.d(TAG, "Menu already showing")
            return
        }
        
        if (!hasOverlayPermission()) {
            Log.w(TAG, "Cannot show menu - no overlay permission")
            return
        }
        
        val wm = windowManager ?: run {
            Log.e(TAG, "WindowManager is null")
            return
        }
        
        try {
            menuView = LayoutInflater.from(this).inflate(R.layout.layout_bubble_menu, null)
            
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            
            setupMenuTabs()
            safeSetupMenuApps()
            
            menuView?.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
                safeHideMenu()
            }
            
            wm.addView(menuView, params)
            isMenuVisible = true
            Log.d(TAG, "Menu shown successfully")
            
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException showing menu: ${e.message}", e)
            menuView = null
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException showing menu: ${e.message}", e)
            menuView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error showing menu: ${e.message}", e)
            menuView = null
        }
    }
    
    private fun safeHideMenu() {
        val view = menuView ?: return
        val wm = windowManager ?: return
        
        try {
            if (view.isAttachedToWindow) {
                wm.removeView(view)
                Log.d(TAG, "Menu removed successfully")
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Menu view not attached")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing menu: ${e.message}", e)
        } finally {
            menuView = null
            isMenuVisible = false
        }
    }
    
    private fun safeSetupMenuApps() {
        try {
            val selectedApps = getSelectedApps()
            val recyclerView = menuView?.findViewById<RecyclerView>(R.id.rvMenuApps)
            val emptyState = menuView?.findViewById<TextView>(R.id.tvEmptyState)
            val appCount = menuView?.findViewById<TextView>(R.id.tvAppCount)
            
            appCount?.text = "${selectedApps.size}"
            
            if (selectedApps.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.VISIBLE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyState?.visibility = View.GONE
                
                recyclerView?.apply {
                    layoutManager = LinearLayoutManager(this@FloatBubbleService)
                    adapter = MenuAppAdapter(selectedApps) { app ->
                        safeLaunchApp(app.packageName)
                        safeHideMenu()
                    }
                }
            }
            
            Log.d(TAG, "Menu apps setup with ${selectedApps.size} apps")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up menu apps: ${e.message}", e)
        }
    }
    
    private fun safeSetupNotifications() {
        try {
            val hasNotificationAccess = isNotificationAccessEnabled()
            val notifications = if (hasNotificationAccess) {
                FloatifyNotificationListener.getNotifications()
            } else {
                emptyList()
            }
            
            val recyclerView = menuView?.findViewById<RecyclerView>(R.id.rvNotifications)
            val emptyState = menuView?.findViewById<TextView>(R.id.tvNoNotifications)
            val accessRequired = menuView?.findViewById<LinearLayout>(R.id.notificationAccessRequired)
            val notificationCount = menuView?.findViewById<TextView>(R.id.tvNotificationCount)
            
            notificationCount?.text = "${notifications.size}"
            
            if (!hasNotificationAccess) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.GONE
                accessRequired?.visibility = View.VISIBLE
                
                accessRequired?.setOnClickListener {
                    openNotificationAccessSettings()
                }
            } else if (notifications.isEmpty()) {
                recyclerView?.visibility = View.GONE
                emptyState?.visibility = View.VISIBLE
                accessRequired?.visibility = View.GONE
            } else {
                recyclerView?.visibility = View.VISIBLE
                emptyState?.visibility = View.GONE
                accessRequired?.visibility = View.GONE
                
                recyclerView?.apply {
                    layoutManager = LinearLayoutManager(this@FloatBubbleService)
                    notificationAdapter = NotificationAdapter(notifications) { notification ->
                        safeLaunchAppFromNotification(notification)
                        safeHideMenu()
                    }
                    adapter = notificationAdapter
                }
            }
            
            Log.d(TAG, "Notifications setup with ${notifications.size} notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications: ${e.message}", e)
        }
    }
    
    private fun safeUpdateNotifications() {
        try {
            if (isMenuVisible && currentTab == 1) {
                val notifications = FloatifyNotificationListener.getNotifications()
                notificationAdapter?.updateNotifications(notifications)
                
                val notificationCount = menuView?.findViewById<TextView>(R.id.tvNotificationCount)
                notificationCount?.text = "${notifications.size}"
                
                val recyclerView = menuView?.findViewById<RecyclerView>(R.id.rvNotifications)
                val emptyState = menuView?.findViewById<TextView>(R.id.tvNoNotifications)
                
                if (notifications.isEmpty()) {
                    recyclerView?.visibility = View.GONE
                    emptyState?.visibility = View.VISIBLE
                } else {
                    recyclerView?.visibility = View.VISIBLE
                    emptyState?.visibility = View.GONE
                }
            }
            safeUpdateBadgeCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications: ${e.message}", e)
        }
    }
    
    private fun isNotificationAccessEnabled(): Boolean {
        return try {
            FloatifyNotificationListener.isListenerConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification access: ${e.message}", e)
            false
        }
    }
    
    private fun openNotificationAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings: ${e.message}", e)
        }
    }
    
    private fun safeLaunchAppFromNotification(notification: NotificationInfo) {
        safeLaunchApp(notification.packageName)
    }
    
    private fun setupMenuTabs() {
        try {
            val tabLayout = menuView?.findViewById<TabLayout>(R.id.tabLayout)
            val appsContainer = menuView?.findViewById<LinearLayout>(R.id.appsContainer)
            val notificationsContainer = menuView?.findViewById<LinearLayout>(R.id.notificationsContainer)
            
            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentTab = tab?.position ?: 0
                    when (currentTab) {
                        0 -> {
                            appsContainer?.visibility = View.VISIBLE
                            notificationsContainer?.visibility = View.GONE
                        }
                        1 -> {
                            appsContainer?.visibility = View.GONE
                            notificationsContainer?.visibility = View.VISIBLE
                            safeSetupNotifications()
                        }
                    }
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            tabLayout?.getTabAt(0)?.select()
            currentTab = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up menu tabs: ${e.message}", e)
        }
    }
    
    private fun safeUpdateMenuApps() {
        try {
            if (isMenuVisible) {
                safeSetupMenuApps()
            }
            safeUpdateBadgeCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating menu apps: ${e.message}", e)
        }
    }
    
    private fun safeUpdateBadgeCount() {
        try {
            val appCount = prefs?.getSelectedApps()?.size ?: 0
            val notificationCount = if (isNotificationAccessEnabled()) {
                FloatifyNotificationListener.getNotificationCount()
            } else {
                0
            }
            val totalCount = appCount + notificationCount
            
            bubbleView?.findViewById<TextView>(R.id.tvBadge)?.apply {
                text = if (notificationCount > 0) notificationCount.toString() else appCount.toString()
                visibility = if (totalCount > 0) View.VISIBLE else View.GONE
            }
            
            bubbleView?.findViewById<View>(R.id.badgeCard)?.visibility = 
                if (totalCount > 0) View.VISIBLE else View.GONE
                
        } catch (e: Exception) {
            Log.e(TAG, "Error updating badge count: ${e.message}", e)
        }
    }
    
    private fun getSelectedApps(): List<AppInfo> {
        return try {
            val pm = packageManager ?: run {
                Log.w(TAG, "PackageManager is null")
                return emptyList()
            }
            val selectedPackages = prefs?.getSelectedApps() ?: run {
                Log.w(TAG, "Prefs is null or no selected apps")
                return emptyList()
            }
            
            selectedPackages.mapNotNull { packageName ->
                try {
                    if (packageName.isNullOrBlank()) {
                        null
                    } else {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        AppInfo(
                            name = pm.getApplicationLabel(appInfo)?.toString() ?: packageName,
                            packageName = packageName,
                            icon = pm.getApplicationIcon(appInfo)
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load app info for $packageName")
                    null
                }
            }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting selected apps: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun safeLaunchApp(packageName: String) {
        try {
            if (packageName.isBlank()) {
                Log.w(TAG, "Cannot launch app - empty package name")
                return
            }
            
            val pm = packageManager ?: run {
                Log.w(TAG, "PackageManager is null")
                return
            }
            
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app $packageName: ${e.message}", e)
        }
    }
}
