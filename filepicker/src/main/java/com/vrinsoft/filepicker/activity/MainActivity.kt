package com.vrinsoft.filepicker.activity

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.databinding.ActivityMainBinding
import com.vrinsoft.filepicker.databinding.ItemPickerOptionBinding
import com.vrinsoft.filepicker.databinding.ItemQuickActionBinding
import com.vrinsoft.filepicker.fragment.PickerBottomSheetFragment
import com.vrinsoft.filepicker.model.ResultFileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), PickerBottomSheetFragment.PickerSheetListener {

    private lateinit var binding: ActivityMainBinding

    private var pendingCameraUri: Uri? = null

    // ── Activity result launchers ────────────────────────────────────────

    private val cameraPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val storagePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.values.all { it }) launchGalleryScreen()
            else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                pendingCameraUri?.let { uri ->
                    openResultsScreen(arrayListOf(
                        ResultFileItem(
                            displayName = "captured_photo.jpg",
                            mimeType    = "image/jpeg",
                            sizeBytes   = 0L,
                            uriString   = uri.toString()
                        )
                    ))
                }
            } else {
                Toast.makeText(this, "Capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Single document picker — opens system file browser.
     * On result: resolve metadata via ContentResolver then go to ResultsActivity.
     */
    private val pickSingleDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            // Persist permission so the URI stays readable across app restarts
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            openResultsScreen(arrayListOf(uriToResultFile(uri)))
        }

    /**
     * Multiple documents picker — opens system file browser with multi-select.
     * On result: resolve metadata for each URI then go to ResultsActivity.
     */
    private val pickMultipleDocumentsLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            // Persist read permission for each URI
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            openResultsScreen(ArrayList(uris.map { uriToResultFile(it) }))
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val files: ArrayList<ResultFileItem>? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        result.data?.getParcelableArrayListExtra(
                            GalleryActivity.KEY_RESULT_FILES, ResultFileItem::class.java)
                    else
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableArrayListExtra(GalleryActivity.KEY_RESULT_FILES)

                if (!files.isNullOrEmpty()) {
                    openResultsScreen(files)
                }
            }
        }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        setupPickerOptions()
        setupQuickActions()
        setupCta()
        setupBottomNav()
    }

    // ── Picker rows ──────────────────────────────────────────────────────

    private fun setupPickerOptions() {
        ItemPickerOptionBinding.bind(binding.rowImageCapture.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_camera)
            setIconColors(this, R.color.icon_bg_blue, R.color.icon_tint_blue)
            tvOptionName.setText(R.string.option_camera)
            tvOptionDesc.setText(R.string.option_camera_desc)
            tvBadge.visibility = View.GONE
            root.setOnClickListener { launchImageCapture() }
        }
        ItemPickerOptionBinding.bind(binding.rowVideoCapture.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_video)
            setIconColors(this, R.color.icon_bg_coral, R.color.icon_tint_coral)
            tvOptionName.setText(R.string.option_video)
            tvOptionDesc.setText(R.string.option_video_desc)
            tvBadge.visibility = View.GONE
            root.setOnClickListener { launchVideoCapture() }
        }
        ItemPickerOptionBinding.bind(binding.rowGallery.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_photo)
            setIconColors(this, R.color.icon_bg_teal, R.color.icon_tint_teal)
            tvOptionName.setText(R.string.option_gallery)
            tvOptionDesc.setText(R.string.option_gallery_desc)
            tvBadge.visibility = View.VISIBLE
            tvBadge.setText(R.string.option_gallery_badge)
            tvBadge.setTextColor(color(R.color.badge_text_new))
            tvBadge.backgroundTintList = ColorStateList.valueOf(color(R.color.badge_bg_new))
            root.setOnClickListener { launchGalleryPicker() }
        }
        ItemPickerOptionBinding.bind(binding.rowDocuments.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_file_text)
            setIconColors(this, R.color.icon_bg_amber, R.color.icon_tint_amber)
            tvOptionName.setText(R.string.option_documents)
            tvOptionDesc.setText(R.string.option_documents_desc)
            tvBadge.visibility = View.GONE
            // Long-press → pick multiple documents
            root.setOnClickListener      { launchDocumentPicker(multiple = false) }
            root.setOnLongClickListener  { launchDocumentPicker(multiple = true); true }
        }
    }

    // ── Quick actions ────────────────────────────────────────────────────

    private fun setupQuickActions() {
        ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaAllFiles.root).apply {
            ivQaIcon.setImageResource(R.drawable.ic_folder_open)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_purple))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_purple))
            tvQaLabel.setText(R.string.qa_all_files)
            tvQaDesc.setText(R.string.qa_all_files_desc)
            root.setOnClickListener { openBottomSheet() }
        }
        ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaSettings.root).apply {
            ivQaIcon.setImageResource(R.drawable.ic_settings)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_blue))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_blue))
            tvQaLabel.setText(R.string.qa_settings)
            tvQaDesc.setText(R.string.qa_settings_desc)
            root.setOnClickListener { openPermissionsScreen() }
        }
        ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaRecent.root).apply {
            ivQaIcon.setImageResource(R.drawable.ic_history)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_green))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_green))
            tvQaLabel.setText(R.string.qa_recent)
            tvQaDesc.setText(R.string.qa_recent_desc)
            root.setOnClickListener { openPermissionsScreen() }
        }
        ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaPermissions.root).apply {
            ivQaIcon.setImageResource(R.drawable.ic_shield_check)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_amber))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_amber))
            tvQaLabel.setText(R.string.qa_permissions)
            tvQaDesc.setText(R.string.qa_permissions_desc)
            root.setOnClickListener { openPermissionsScreen() }
        }
    }

    private fun setupCta() {
        binding.btnOpenPicker.setOnClickListener { openBottomSheet() }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home   -> true
                R.id.nav_files  -> { launchGalleryScreen(); true }
                R.id.nav_recent -> { openPermissionsScreen(); true }
                else            -> false
            }
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────

    private fun openBottomSheet() {
        PickerBottomSheetFragment
            .newInstance(this)
            .show(supportFragmentManager, PickerBottomSheetFragment.TAG)
    }

    private fun launchGalleryScreen(maxSelection: Int = Int.MAX_VALUE) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra(GalleryActivity.KEY_MAX_SELECTION, maxSelection)
        }
        galleryLauncher.launch(intent)        // ← result comes back here
    }

    /** ✅ Navigates to ResultsActivity with the selected/captured files */
    private fun openResultsScreen(files: ArrayList<ResultFileItem>) {
        startActivity(Intent(this, ResultsActivity::class.java).apply {
            putParcelableArrayListExtra(ResultsActivity.KEY_SELECTED_FILES, files)
        })
    }

    /** Navigates to PermissionsActivity (no files — for settings/permissions management) */
    private fun openPermissionsScreen(files: ArrayList<ResultFileItem>? = null) {
        startActivity(Intent(this, PermissionsActivity::class.java).apply {
            files?.let { putParcelableArrayListExtra(PermissionsActivity.KEY_PICKED_FILES, it) }
        })
    }

    // ── PickerSheetListener callbacks ────────────────────────────────────

    override fun onCameraSelected()   { launchImageCapture() }
    override fun onVideoSelected()    { launchVideoCapture() }
    override fun onGallerySelected()  { launchGalleryPicker() }
    /** ✅ Sheet → document → single pick → ResultsActivity */
    override fun onDocumentSelected() { launchDocumentPicker(multiple = false) }

    // ── Launch logic ─────────────────────────────────────────────────────

    private fun launchImageCapture() {
        if (hasPerm(Manifest.permission.CAMERA)) startCamera()
        else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchVideoCapture() {
        // Reuse gallery filtered to videos until TakeVideo contract is added
        launchGalleryPicker()
    }

    private fun launchGalleryPicker() {
        if (hasStoragePermission()) launchGalleryScreen()
        else storagePermLauncher.launch(storagePermissions())
    }

    /**
     * Document picker using SAF — no storage permission needed.
     * [multiple] = false → single file (OpenDocument)
     * [multiple] = true  → multi-file (OpenMultipleDocuments)
     *
     * SAF supported MIME types:
     *   PDF, Word, Excel, PowerPoint, text, and a wildcard fallback.
     */
    private fun launchDocumentPicker(multiple: Boolean) {
        val mimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "*/*"
        )
        if (multiple) {
            pickMultipleDocumentsLauncher.launch(mimeTypes)
        } else {
            pickSingleDocumentLauncher.launch(mimeTypes)
        }
    }

    // ── Camera helpers ───────────────────────────────────────────────────

    private fun startCamera() {
        pendingCameraUri = createImageUri()
        if (pendingCameraUri != null) {
            takePictureLauncher.launch(pendingCameraUri!!)
        } else {
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageUri(): Uri? {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/FilePicker")
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilePicker")
            dir.mkdirs()
            FileProvider.getUriForFile(
                this, "${packageName}.filepicker.provider", File(dir, filename)
            )
        }
    }

    // ── URI → ResultFileItem ─────────────────────────────────────────────

    /**
     * Resolves display name, MIME type, and size for any content:// URI.
     * Works for both SAF document URIs and MediaStore URIs.
     */
    private fun uriToResultFile(uri: Uri): ResultFileItem {
        var name = uri.lastPathSegment ?: "document"
        var mime = "application/octet-stream"
        var size = 0L

        // Try ContentResolver first (works for all content:// URIs)
        contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: name
                mime = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: mime
                size = cursor.getLong(2)
            }
        }

        // Fallback: try to get MIME type directly from ContentResolver
        if (mime == "application/octet-stream") {
            contentResolver.getType(uri)?.let { mime = it }
        }

        return ResultFileItem(
            displayName = name,
            mimeType    = mime,
            sizeBytes   = size,
            uriString   = uri.toString()
        )
    }

    // ── Permission helpers ───────────────────────────────────────────────

    private fun hasPerm(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasStoragePermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            hasPerm(Manifest.permission.READ_MEDIA_IMAGES)
        else
            hasPerm(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun storagePermissions() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    // ── Misc helpers ─────────────────────────────────────────────────────

    private fun setIconColors(b: ItemPickerOptionBinding, bgRes: Int, tintRes: Int) {
        b.ivOptionIcon.backgroundTintList = ColorStateList.valueOf(color(bgRes))
        b.ivOptionIcon.imageTintList      = ColorStateList.valueOf(color(tintRes))
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)
}