package com.vrinsoft.filepicker.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.adapter.ResultFilesAdapter
import com.vrinsoft.filepicker.databinding.ActivityResultsBinding
import com.vrinsoft.filepicker.model.ResultFileItem

/**
 * ResultsActivity — Screen 4
 *
 * Receives selected files from GalleryActivity (or camera/document picker)
 * and displays them as a list with:
 *   - Thumbnail (images/videos via Coil)
 *   - MIME icon (documents)
 *   - Filename, size, duration
 *   - Remove button per item
 *   - Summary card (count + total size)
 *   - "Pick more" → back to GalleryActivity
 *   - "Use files" → pass final list to your app logic
 */
class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding

    // Mutable so user can remove items before confirming
    private val selectedFiles = mutableListOf<ResultFileItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Receive files from Intent ────────────────────────────────────
        val incoming: ArrayList<ResultFileItem>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableArrayListExtra(KEY_SELECTED_FILES, ResultFileItem::class.java)
            else
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(KEY_SELECTED_FILES)

        selectedFiles.addAll(incoming ?: emptyList())

        setupToolbar()
        setupSummaryCard()
        setupResultList()
        setupBottomActions()
    }

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Summary card ─────────────────────────────────────────────────────

    private fun setupSummaryCard() {
        refreshSummary()
    }

    private fun refreshSummary() {
        val count     = selectedFiles.size
        val totalSize = selectedFiles.sumOf { it.sizeBytes }

        binding.tvSummaryCount.text = resources.getQuantityString(
            R.plurals.summary_file_count, count, count
        )
        binding.tvSummarySize.text = getString(
            R.string.summary_total_size, formatSize(totalSize)
        )
    }

    // ── Result list ──────────────────────────────────────────────────────

    private fun setupResultList() {
        val adapter = ResultFilesAdapter(
            items      = selectedFiles,
            onRemove   = { item -> removeItem(item) }
        )
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun removeItem(item: ResultFileItem) {
        selectedFiles.remove(item)
        // Re-submit updated list
        (binding.rvResults.adapter as? ResultFilesAdapter)?.updateItems(selectedFiles.toList())
        refreshSummary()

        // If all items removed, go back to gallery
        if (selectedFiles.isEmpty()) finish()
    }

    // ── Bottom actions ───────────────────────────────────────────────────

    private fun setupBottomActions() {
        // "Pick more" → go back to gallery, preserving current selection
        binding.btnPickMore.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        // "Use files" → your app consumes the final list here
        binding.btnUseFiles.setOnClickListener {
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ── Option A: pass back to calling Activity via setResult ────
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(RESULT_SELECTED_FILES, ArrayList(selectedFiles))
            }
            setResult(RESULT_OK, resultIntent)
            finish()

            // ── Option B: log / share / upload ───────────────────────────
            // selectedFiles.forEach { Log.d("Results", "uri=${it.uriString}") }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.0f KB".format(kb)
            else      -> "$bytes B"
        }
    }

    companion object {
        const val KEY_SELECTED_FILES  = "key_selected_files"
        const val RESULT_SELECTED_FILES = "result_selected_files"
    }
}