package com.floatify.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.floatify.app.util.PrefsManager

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PrefsManager(context)
            
            if (prefs.isBubbleActive() && Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, FloatBubbleService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
