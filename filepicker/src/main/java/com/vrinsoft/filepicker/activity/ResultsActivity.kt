package com.vrinsoft.filepicker.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.adapter.ResultFilesAdapter
import com.vrinsoft.filepicker.databinding.ActivityResultsBinding
import com.vrinsoft.filepicker.model.ResultFileItem

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private val selectedFiles = mutableListOf<ResultFileItem>()

    // ── Launcher for "Pick more" ─────────────────────────────────────────
    // ✅ Uses ActivityResultLauncher so we get files BACK from GalleryActivity
    // and can MERGE them with what is already in selectedFiles.
    private val pickMoreLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newFiles: ArrayList<ResultFileItem>? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        result.data?.getParcelableArrayListExtra(
                            GalleryActivity.KEY_RESULT_FILES, ResultFileItem::class.java)
                    else
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableArrayListExtra(GalleryActivity.KEY_RESULT_FILES)

                if (!newFiles.isNullOrEmpty()) {
                    mergeFiles(newFiles)
                }
            }
        }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun setupSummaryCard() { refreshSummary() }

    private fun refreshSummary() {
        val count     = selectedFiles.size
        val totalSize = selectedFiles.sumOf { it.sizeBytes }
        binding.tvSummaryCount.text = resources.getQuantityString(
            R.plurals.summary_file_count, count, count
        )
        binding.tvSummarySize.text = getString(R.string.summary_total_size, formatSize(totalSize))
    }

    // ── Result list ──────────────────────────────────────────────────────

    private fun setupResultList() {
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = ResultFilesAdapter(
            items    = selectedFiles,
            onRemove = { item -> removeItem(item) }
        )
    }

    private fun removeItem(item: ResultFileItem) {
        selectedFiles.remove(item)
        (binding.rvResults.adapter as? ResultFilesAdapter)?.updateItems(selectedFiles.toList())
        refreshSummary()
        if (selectedFiles.isEmpty()) finish()
    }

    // ── Merge logic ───────────────────────────────────────────────────────

    /**
     * ✅ Merges newly picked files into the existing list.
     * - Skips duplicates (matched by uriString)
     * - Appends only net-new files
     * - Refreshes the adapter and summary card in place — no new Activity instance
     */
    private fun mergeFiles(newFiles: List<ResultFileItem>) {
        val existingUris = selectedFiles.map { it.uriString }.toSet()
        val toAdd = newFiles.filter { it.uriString !in existingUris }

        if (toAdd.isEmpty()) {
            Toast.makeText(this, "No new files added", Toast.LENGTH_SHORT).show()
            return
        }

        selectedFiles.addAll(toAdd)
        (binding.rvResults.adapter as? ResultFilesAdapter)?.updateItems(selectedFiles.toList())
        refreshSummary()

        // Scroll to the first newly added item
        val firstNewIndex = selectedFiles.size - toAdd.size
        binding.rvResults.smoothScrollToPosition(firstNewIndex)
    }

    // ── Bottom actions ────────────────────────────────────────────────────

    private fun setupBottomActions() {
        // ✅ "Pick more" — opens GalleryActivity with CURRENT selection pre-loaded
        // Uses pickMoreLauncher so result comes back HERE and gets merged
        binding.btnPickMore.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java).apply {
                // Pass current files so GalleryActivity shows them as already selected
                putParcelableArrayListExtra(
                    GalleryActivity.KEY_PRESELECTED_FILES,
                    ArrayList(selectedFiles)
                )
            }
            pickMoreLauncher.launch(intent)
        }

        // "Use files" — return final list to the calling activity
        binding.btnUseFiles.setOnClickListener {
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(RESULT_SELECTED_FILES, ArrayList(selectedFiles))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
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
        const val KEY_SELECTED_FILES    = "key_selected_files"
        const val RESULT_SELECTED_FILES = "result_selected_files"

        fun newIntent(context: Context, maxSelection: Int = Int.MAX_VALUE): Intent {
            return Intent(context, ResultsActivity::class.java).apply {
                putExtra("key_max_selection", maxSelection)
            }
        }
    }
}