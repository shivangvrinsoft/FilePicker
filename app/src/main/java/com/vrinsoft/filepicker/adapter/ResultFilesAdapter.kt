package com.vrinsoft.filepicker.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.databinding.ItemResultFileBinding
import com.vrinsoft.filepicker.model.ResultFileItem

/**
 * ResultFilesAdapter
 *
 * onRemove is optional (defaults to no-op) so the adapter can be used:
 *   - With remove button  →  ResultsActivity(items, onRemove = { removeItem(it) })
 *   - Without remove button → PermissionsActivity(items)  [no second arg needed]
 */
class ResultFilesAdapter(
    items: List<ResultFileItem>,
    private val onRemove: ((ResultFileItem) -> Unit)? = null   // ← optional
) : ListAdapter<ResultFileItem, ResultFilesAdapter.ResultViewHolder>(DIFF_CALLBACK) {

    init { submitList(items.toList()) }

    inner class ResultViewHolder(
        private val binding: ItemResultFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ResultFileItem) {

            // ── Text ─────────────────────────────────────────────────────
            binding.tvFileName.text = item.displayName
            binding.tvFileMeta.text = "${item.formattedSize} · ${item.mimeType}"

            // ── Duration (videos only) ───────────────────────────────────
            if (item.isVideo && item.durationMs > 0) {
                binding.tvFileDuration.visibility = View.VISIBLE
                binding.tvFileDuration.text = "▶ ${formatDuration(item.durationMs)}"
            } else {
                binding.tvFileDuration.visibility = View.GONE
            }

            // ── Thumbnail vs MIME icon ───────────────────────────────────
            when {
                item.isImage || item.isVideo -> {
                    binding.ivFileThumbnail.visibility = View.VISIBLE
                    binding.ivFileMimeIcon.visibility  = View.GONE
                    binding.ivFileThumbnail.load(item.uriString.toUri()) {
                        crossfade(true)
                        scale(Scale.FILL)
                        placeholder(R.drawable.ic_photo)
                        error(R.drawable.ic_photo)
                    }
                    binding.cvFileThumbnail.setCardBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            if (item.isVideo) R.color.icon_bg_coral else R.color.icon_bg_teal
                        )
                    )
                }
                else -> {
                    // Document — MIME icon fallback
                    binding.ivFileThumbnail.visibility = View.GONE
                    binding.ivFileMimeIcon.visibility  = View.VISIBLE
                    binding.ivFileMimeIcon.setImageResource(R.drawable.ic_file_text)
                    binding.ivFileMimeIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(binding.root.context, R.color.icon_tint_amber)
                    )
                    binding.cvFileThumbnail.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.icon_bg_amber)
                    )
                }
            }

            // ── Remove button ────────────────────────────────────────────
            // Hide the button entirely if no onRemove callback was provided
            if (onRemove != null) {
                binding.ivResultStatus.visibility = View.VISIBLE
                binding.ivResultStatus.setOnClickListener { onRemove.invoke(item) }
            } else {
                binding.ivResultStatus.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemResultFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun updateItems(newItems: List<ResultFileItem>) = submitList(newItems.toList())

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