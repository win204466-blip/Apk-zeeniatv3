package com.floatify.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.floatify.app.R
import com.floatify.app.util.PrefsManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    private lateinit var prefs: PrefsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_splash)
            prefs = PrefsManager(this)
            
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNext()
            }, 2500)
        } catch (e: Exception) {
            e.printStackTrace()
            navigateToNext()
        }
    }
    
    private fun navigateToNext() {
        try {
            val intent = if (::prefs.isInitialized && prefs.isOnboardingDone()) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
