package com.floatify.app.service

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.floatify.app.data.AppInfo
import com.floatify.app.databinding.ItemMenuAppBinding

class MenuAppAdapter(
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<MenuAppAdapter.ViewHolder>() {
    
    companion object {
        private const val TAG = "MenuAppAdapter"
    }
    
    inner class ViewHolder(private val binding: ItemMenuAppBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(app: AppInfo?) {
            if (app == null) {
                Log.w(TAG, "Attempted to bind null app")
                return
            }
            
            try {
                binding.tvAppName.text = app.name ?: "Unknown"
                
                if (app.icon != null) {
                    binding.ivAppIcon.setImageDrawable(app.icon)
                } else {
                    binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }
                
                binding.root.setOnClickListener {
                    try {
                        onAppClick(app)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in app click callback: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding app: ${e.message}", e)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val binding = ItemMenuAppBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ViewHolder(binding)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}", e)
            throw e
        }
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position >= 0 && position < apps.size) {
                holder.bind(apps[position])
            } else {
                Log.w(TAG, "Invalid position: $position, apps size: ${apps.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}", e)
        }
    }
    
    override fun getItemCount(): Int {
        return try {
            apps.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item count: ${e.message}", e)
            0
        }
    }
}
