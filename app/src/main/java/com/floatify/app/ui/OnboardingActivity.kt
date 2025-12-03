package com.floatify.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.chip.Chip
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.floatify.app.R
import com.floatify.app.databinding.ActivityOnboardingBinding
import com.floatify.app.util.PrefsManager

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PrefsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityOnboardingBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            prefs = PrefsManager(this)
            
            setupViewPager()
            setupButtons()
            setupIndicators()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
    
    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                updateButtons(position)
            }
        })
    }
    
    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < 2) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
        
        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }
    
    private fun setupIndicators() {
        repeat(3) { index ->
            val dot = View(this).apply {
                layoutParams = ViewGroup.MarginLayoutParams(24, 24).apply {
                    marginStart = if (index > 0) 16 else 0
                }
                background = ContextCompat.getDrawable(
                    this@OnboardingActivity,
                    if (index == 0) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            }
            binding.indicator.addView(dot)
        }
    }
    
    private fun updateIndicators(position: Int) {
        for (i in 0 until binding.indicator.childCount) {
            val dot = binding.indicator.getChildAt(i)
            dot.background = ContextCompat.getDrawable(
                this,
                if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
            )
        }
    }
    
    private fun updateButtons(position: Int) {
        binding.btnNext.text = if (position == 2) getString(R.string.btn_start) else getString(R.string.btn_next)
        binding.btnSkip.visibility = if (position == 2) View.GONE else View.VISIBLE
    }
    
    private fun finishOnboarding() {
        prefs.setOnboardingDone(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    class OnboardingAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 3
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WelcomeFragment()
                1 -> PermissionFragment()
                else -> ThemeFragment()
            }
        }
    }
    
    class WelcomeFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? = inflater.inflate(R.layout.fragment_onboarding_welcome, container, false)
    }
    
    class PermissionFragment : Fragment() {
        
        private var chipOverlayStatus: Chip? = null
        private var chipNotificationStatus: Chip? = null
        private var chipNotificationAccessStatus: Chip? = null
        
        private val overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updatePermissionStatus()
        }
        
        private val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            updatePermissionStatus()
        }
        
        private val notificationAccessLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updatePermissionStatus()
        }
        
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_onboarding_permission, container, false)
            
            chipOverlayStatus = view.findViewById(R.id.tvOverlayStatus)
            chipNotificationStatus = view.findViewById(R.id.tvNotificationStatus)
            chipNotificationAccessStatus = view.findViewById(R.id.tvNotificationAccessStatus)
            
            view.findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
                requestPermissions()
            }
            
            view.findViewById<Button>(R.id.btnGrantNotificationAccess).setOnClickListener {
                requestNotificationAccess()
            }
            
            return view
        }
        
        override fun onResume() {
            super.onResume()
            updatePermissionStatus()
        }
        
        private fun updatePermissionStatus() {
            val ctx = context ?: return
            
            val hasOverlay = Settings.canDrawOverlays(ctx)
            chipOverlayStatus?.apply {
                text = if (hasOverlay) getString(R.string.permission_granted) else getString(R.string.permission_denied)
                setTextColor(ContextCompat.getColor(ctx, if (hasOverlay) R.color.success else R.color.error))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, if (hasOverlay) R.color.success_container else R.color.error_container)
                )
            }
            
            val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            
            chipNotificationStatus?.apply {
                text = if (hasNotification) getString(R.string.permission_granted) else getString(R.string.permission_denied)
                setTextColor(ContextCompat.getColor(ctx, if (hasNotification) R.color.success else R.color.error))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, if (hasNotification) R.color.success_container else R.color.error_container)
                )
            }
            
            val hasNotificationAccess = isNotificationListenerEnabled(ctx)
            chipNotificationAccessStatus?.apply {
                text = if (hasNotificationAccess) getString(R.string.permission_granted) else getString(R.string.permission_denied)
                setTextColor(ContextCompat.getColor(ctx, if (hasNotificationAccess) R.color.success else R.color.error))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, if (hasNotificationAccess) R.color.success_container else R.color.error_container)
                )
            }
        }
        
        private fun isNotificationListenerEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(packageName) == true
        }
        
        private fun requestPermissions() {
            val ctx = context ?: return
            
            if (!Settings.canDrawOverlays(ctx)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                )
                overlayPermissionLauncher.launch(intent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        private fun requestNotificationAccess() {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            notificationAccessLauncher.launch(intent)
        }
    }
    
    class ThemeFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_onboarding_theme, container, false)
            
            val prefs = PrefsManager(requireContext())
            val radioGroup = view.findViewById<RadioGroup>(R.id.rgTheme)
            
            when (prefs.getThemeMode()) {
                PrefsManager.THEME_LIGHT -> radioGroup.check(R.id.rbLight)
                PrefsManager.THEME_DARK -> radioGroup.check(R.id.rbDark)
                else -> radioGroup.check(R.id.rbAuto)
            }
            
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val mode = when (checkedId) {
                    R.id.rbLight -> PrefsManager.THEME_LIGHT
                    R.id.rbDark -> PrefsManager.THEME_DARK
                    else -> PrefsManager.THEME_AUTO
                }
                prefs.setThemeMode(mode)
                
                when (mode) {
                    PrefsManager.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    PrefsManager.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            
            return view
        }
    }
}
