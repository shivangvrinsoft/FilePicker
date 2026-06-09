package com.vrinsoft.filepickerdemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vrinsoft.filepicker.model.ResultFileItem
import com.vrinsoft.filepickerdemo.databinding.ItemSelectedFileBinding
import androidx.core.net.toUri

class SelectedFilesAdapter :
    ListAdapter<ResultFileItem, SelectedFilesAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemSelectedFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultFileItem) {
            binding.tvFileName.text = item.displayName
            binding.tvMimeType.text = item.mimeType
            binding.tvFileSize.text = formatSize(item.sizeBytes)
            binding.ivThumbnail.load(item.uriString.toUri()) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectedFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.0f KB".format(kb)
            else -> "$bytes B"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ResultFileItem>() {
        override fun areItemsTheSame(a: ResultFileItem, b: ResultFileItem) =
            a.uriString == b.uriString
        override fun areContentsTheSame(a: ResultFileItem, b: ResultFileItem) =
            a == b
    }
}