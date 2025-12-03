package com.floatify.app.data

import android.graphics.drawable.Drawable

data class NotificationInfo(
    val key: String,
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val title: String,
    val text: String,
    val postTime: Long,
    val timeString: String
)
