package com.floatify.app.service

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.floatify.app.data.NotificationInfo
import com.floatify.app.databinding.ItemNotificationBinding

class NotificationAdapter(
    private var notifications: List<NotificationInfo>,
    private val onNotificationClick: (NotificationInfo) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    
    companion object {
        private const val TAG = "NotificationAdapter"
    }
    
    inner class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(notification: NotificationInfo?) {
            if (notification == null) {
                Log.w(TAG, "Attempted to bind null notification")
                return
            }
            
            try {
                binding.tvAppName.text = notification.appName
                binding.tvTitle.text = notification.title.ifEmpty { notification.appName }
                binding.tvText.text = notification.text
                binding.tvTime.text = notification.timeString
                
                if (notification.appIcon != null) {
                    binding.ivAppIcon.setImageDrawable(notification.appIcon)
                } else {
                    binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }
                
                binding.root.setOnClickListener {
                    try {
                        onNotificationClick(notification)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in notification click callback: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding notification: ${e.message}", e)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val binding = ItemNotificationBinding.inflate(
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
            if (position >= 0 && position < notifications.size) {
                holder.bind(notifications[position])
            } else {
                Log.w(TAG, "Invalid position: $position, notifications size: ${notifications.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position: ${e.message}", e)
        }
    }
    
    override fun getItemCount(): Int {
        return try {
            notifications.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item count: ${e.message}", e)
            0
        }
    }
    
    fun updateNotifications(newNotifications: List<NotificationInfo>) {
        try {
            val diffCallback = NotificationDiffCallback(notifications, newNotifications)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            notifications = newNotifications
            diffResult.dispatchUpdatesTo(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications: ${e.message}", e)
            notifications = newNotifications
            notifyDataSetChanged()
        }
    }
    
    private class NotificationDiffCallback(
        private val oldList: List<NotificationInfo>,
        private val newList: List<NotificationInfo>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize() = oldList.size
        
        override fun getNewListSize() = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].key == newList[newItemPosition].key
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.title == new.title && 
                   old.text == new.text && 
                   old.postTime == new.postTime
        }
    }
}
