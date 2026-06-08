package com.vrinsoft.filepicker.activity

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.adapter.GalleryAdapter
import com.vrinsoft.filepicker.databinding.ActivityGalleryBinding
import com.vrinsoft.filepicker.databinding.LayoutGalleryConfirmBarBinding
import com.vrinsoft.filepicker.model.ResultFileItem

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var confirmBar: LayoutGalleryConfirmBarBinding
    private lateinit var adapter: GalleryAdapter

    private var maxSelection: Int = Int.MAX_VALUE
    private val selectedItems = mutableListOf<ResultFileItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        confirmBar = LayoutGalleryConfirmBarBinding.bind(binding.confirmBar.root)
        maxSelection = intent.getIntExtra(KEY_MAX_SELECTION, Int.MAX_VALUE)

        setupToolbar()
        setupGalleryGrid()
        setupFilterChips()
        setupConfirmBar()
        checkAndRequestPermission()
    }

    // ── Permission ───────────────────────────────────────────────────────

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.values.all { it }) loadMedia(MediaFilter.ALL)
        }

    private fun checkAndRequestPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) loadMedia(MediaFilter.ALL) else permissionLauncher.launch(permissions)
    }

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Gallery grid ─────────────────────────────────────────────────────

    private fun setupGalleryGrid() {
        adapter = GalleryAdapter(onItemClick = { toggleSelection(it) })
        binding.rvGallery.layoutManager = GridLayoutManager(this, GRID_SPAN)
        binding.rvGallery.adapter = adapter
    }

    // ── Filter chips ─────────────────────────────────────────────────────

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, ids ->
            val filter = when (ids.firstOrNull()) {
                R.id.chipImages    -> MediaFilter.IMAGES
                R.id.chipVideos    -> MediaFilter.VIDEOS
                R.id.chipDocuments -> MediaFilter.DOCUMENTS
                else               -> MediaFilter.ALL
            }
            loadMedia(filter)
        }
    }

    // ── Confirm bar ───────────────────────────────────────────────────────

    private fun setupConfirmBar() {
        updateConfirmBar()

        confirmBar.btnClearSelection.setOnClickListener {
            selectedItems.clear()
            adapter.clearSelections()
            updateConfirmBar()
        }

        // ✅ CONFIRM → navigate to ResultsActivity with selected items
        confirmBar.btnConfirm.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                val intent = Intent(this, ResultsActivity::class.java).apply {
                    putParcelableArrayListExtra(
                        ResultsActivity.KEY_SELECTED_FILES,
                        ArrayList(selectedItems)
                    )
                }
                startActivity(intent)
            }
        }
    }

    // ── Selection ────────────────────────────────────────────────────────

    private fun toggleSelection(item: ResultFileItem) {
        if (selectedItems.contains(item)) selectedItems.remove(item)
        else {
            if (selectedItems.size >= maxSelection) return
            selectedItems.add(item)
        }
        adapter.updateSelections(selectedItems.toList())
        updateConfirmBar()
    }

    private fun updateConfirmBar() {
        val count = selectedItems.size
        confirmBar.tvSelectionCount.text =
            if (count == 0) getString(R.string.selection_count_zero)
            else getString(R.string.selection_count, count)
        confirmBar.btnClearSelection.visibility = if (count > 0) View.VISIBLE else View.GONE
        confirmBar.btnConfirm.isEnabled = count > 0
    }

    // ── Media loader ─────────────────────────────────────────────────────

    private fun loadMedia(filter: MediaFilter) {
        val list = mutableListOf<ResultFileItem>()
        if (filter == MediaFilter.ALL || filter == MediaFilter.IMAGES) list += queryImages()
        if (filter == MediaFilter.ALL || filter == MediaFilter.VIDEOS) list += queryVideos()
        if (filter == MediaFilter.DOCUMENTS) list += queryDocuments()
        list.sortByDescending { it.dateAdded }
        Log.d(TAG, "loadMedia: total=${list.size}, filter=$filter")
        adapter.submitList(list)
    }

    private fun queryImages(): List<ResultFileItem> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val result = mutableListOf<ResultFileItem>()
        contentResolver.query(collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(ResultFileItem(
                    displayName = cursor.getString(nameCol) ?: "image",
                    mimeType    = cursor.getString(mimeCol) ?: "image/jpeg",
                    sizeBytes   = cursor.getLong(sizeCol),
                    uriString   = uri.toString(),
                    durationMs  = 0L,
                    dateAdded   = cursor.getLong(dateCol)
                ))
            }
        }
        return result
    }

    private fun queryVideos(): List<ResultFileItem> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        val result = mutableListOf<ResultFileItem>()
        contentResolver.query(collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(ResultFileItem(
                    displayName = cursor.getString(nameCol) ?: "video",
                    mimeType    = cursor.getString(mimeCol) ?: "video/mp4",
                    sizeBytes   = cursor.getLong(sizeCol),
                    uriString   = uri.toString(),
                    durationMs  = cursor.getLong(durCol),
                    dateAdded   = cursor.getLong(dateCol)
                ))
            }
        }
        return result
    }

    private fun queryDocuments(): List<ResultFileItem> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}" +
            " AND (${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/%'" +
            " OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%')"
        val result = mutableListOf<ResultFileItem>()
        contentResolver.query(collection, projection, selection, null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(ResultFileItem(
                    displayName = cursor.getString(nameCol) ?: "document",
                    mimeType    = cursor.getString(mimeCol) ?: "application/octet-stream",
                    sizeBytes   = cursor.getLong(sizeCol),
                    uriString   = uri.toString(),
                    durationMs  = 0L,
                    dateAdded   = cursor.getLong(dateCol)
                ))
            }
        }
        return result
    }

    enum class MediaFilter { ALL, IMAGES, VIDEOS, DOCUMENTS }

    companion object {
        const val KEY_MAX_SELECTION = "key_max_selection"
        private const val GRID_SPAN = 3
        private const val TAG = "GalleryActivity"
    }
}