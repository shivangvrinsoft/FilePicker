package com.vrinsoft.filepicker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import androidx.core.net.toUri
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.databinding.ItemGalleryMediaBinding
import com.vrinsoft.filepicker.model.ResultFileItem

class GalleryAdapter(
    private val onItemClick: (ResultFileItem) -> Unit
) : ListAdapter<ResultFileItem, GalleryAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    private val selectedItems = mutableListOf<ResultFileItem>()

    // ── ViewHolder ───────────────────────────────────────────────────────

    inner class MediaViewHolder(
        private val binding: ItemGalleryMediaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultFileItem) {
            val selectedIndex = selectedItems.indexOfFirst { it.uriString == item.uriString }
            val isSelected    = selectedIndex >= 0

            // Load thumbnail using the correct content:// URI
            // Scale.FILL ensures the image crops to fill the square cell
            binding.ivThumbnail.load(item.uriString.toUri()) {
                crossfade(true)
                scale(Scale.FILL)
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
            }

            // Selection overlay
            binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Numbered badge vs empty ring
            if (isSelected) {
                binding.cvSelectionBadge.visibility  = View.VISIBLE
                binding.cvUnselectedRing.visibility  = View.GONE
                binding.tvSelectionNumber.text        = (selectedIndex + 1).toString()
            } else {
                binding.cvSelectionBadge.visibility  = View.GONE
                binding.cvUnselectedRing.visibility  = View.VISIBLE
            }

            // Video duration chip
            binding.tvDuration.visibility = if (item.durationMs > 0) View.VISIBLE else View.GONE
            if (item.durationMs > 0) {
                binding.tvDuration.text = formatDuration(item.durationMs)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    // ── Adapter overrides ────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemGalleryMediaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        // ✅ Force square cell: width = height = screenWidth / 3
        val cellSize = parent.measuredWidth.takeIf { it > 0 }
            ?: parent.context.resources.displayMetrics.widthPixels
        val size = cellSize / 3
        binding.root.layoutParams = RecyclerView.LayoutParams(size, size)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── Selection API ────────────────────────────────────────────────────

    fun updateSelections(newSelections: List<ResultFileItem>) {
        val previous = selectedItems.toList()
        selectedItems.clear()
        selectedItems.addAll(newSelections)
        (previous + newSelections)
            .distinctBy { it.uriString }
            .forEach { item ->
                val pos = currentList.indexOfFirst { it.uriString == item.uriString }
                if (pos >= 0) notifyItemChanged(pos)
            }
    }

    fun clearSelections() {
        val previous = selectedItems.toList()
        selectedItems.clear()
        previous.forEach { item ->
            val pos = currentList.indexOfFirst { it.uriString == item.uriString }
            if (pos >= 0) notifyItemChanged(pos)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun formatDuration(ms: Long): String {
        val sec = ms / 1000
        return "%d:%02d".format(sec / 60, sec % 60)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ResultFileItem>() {
            override fun areItemsTheSame(o: ResultFileItem, n: ResultFileItem) =
                o.uriString == n.uriString
            override fun areContentsTheSame(o: ResultFileItem, n: ResultFileItem) =
                o == n
        }
    }
}