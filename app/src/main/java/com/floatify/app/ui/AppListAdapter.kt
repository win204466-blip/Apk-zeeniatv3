package com.floatify.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.floatify.app.data.AppInfo
import com.floatify.app.databinding.ItemAppBinding

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onAppSelected: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    
    inner class ViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(app: AppInfo) {
            binding.cbSelected.setOnCheckedChangeListener(null)
            
            binding.tvAppName.text = app.name
            binding.tvPackageName.text = app.packageName
            binding.ivAppIcon.setImageDrawable(app.icon)
            binding.cbSelected.isChecked = app.isSelected
            
            binding.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                if (app.isSelected != isChecked) {
                    app.isSelected = isChecked
                    onAppSelected(app, isChecked)
                }
            }
            
            binding.root.setOnClickListener {
                binding.cbSelected.isChecked = !binding.cbSelected.isChecked
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position])
    }
    
    override fun getItemCount() = apps.size
}
