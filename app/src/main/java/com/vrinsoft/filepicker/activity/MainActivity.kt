package com.vrinsoft.filepicker.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vrinsoft.filepicker.fragment.PickerBottomSheetFragment
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.databinding.ActivityMainBinding
import com.vrinsoft.filepicker.databinding.ItemPickerOptionBinding
import com.vrinsoft.filepicker.databinding.ItemQuickActionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), PickerBottomSheetFragment.PickerSheetListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageUri: Uri

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

    // ── Picker option rows ───────────────────────────────────────────────

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
            root.setOnClickListener { launchDocumentPicker() }
        }
    }

    // ── Quick action cards ───────────────────────────────────────────────

    private fun setupQuickActions() {
        ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaAllFiles.root).apply {
            ivQaIcon.setImageResource(R.drawable.ic_folder_open)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_purple))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_purple))
            tvQaLabel.setText(R.string.qa_all_files)
            tvQaDesc.setText(R.string.qa_all_files_desc)
            root.setOnClickListener { launchAllFilePicker() }
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

    // ── CTA ──────────────────────────────────────────────────────────────

    private fun setupCta() {
        binding.btnOpenPicker.setOnClickListener { openBottomSheet() }
    }

    // ── Bottom nav ───────────────────────────────────────────────────────

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home   -> true
                R.id.nav_files  -> { openGalleryScreen(); true }
                R.id.nav_recent -> { openPermissionsScreen(); true }
                else            -> false
            }
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────

    /** Screen 2 — Bottom sheet picker */
    private fun openBottomSheet() {
        PickerBottomSheetFragment
            .newInstance(this)
            .show(supportFragmentManager, PickerBottomSheetFragment.TAG)
    }

    /** Screen 3 — Multi-select gallery */
    private fun openGalleryScreen(maxSelection: Int = Int.MAX_VALUE) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra(GalleryActivity.KEY_MAX_SELECTION, maxSelection)
        }
        startActivity(intent)
    }

    /** Screen 4 — Permissions & results */
    private fun openPermissionsScreen() {
        startActivity(Intent(this, PermissionsActivity::class.java))
    }

    // ── PickerSheetListener (Screen 2 callbacks) ─────────────────────────

    override fun onCameraSelected()   { launchImageCapture() }
    override fun onVideoSelected()    { launchVideoCapture() }
    override fun onGallerySelected()  { openGalleryScreen() }
    override fun onDocumentSelected() { launchDocumentPicker() }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val storageDir = cacheDir

        return File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private val takePictureLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {
                openGalleryScreen()

                Toast.makeText(
                    this,
                    "Image Captured Successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Debug
                println("Captured: $imageUri")
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                openCamera()
            }
        }

    private fun openCamera() {
        imageUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            createImageFile()
        )

        takePictureLauncher.launch(imageUri)
    }

    // ── Picker launch stubs ──────────────────────────────────────────────
    private fun launchImageCapture() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(
                android.Manifest.permission.CAMERA
            )
        }
    }

    private fun launchVideoCapture()   { /* TODO: ActivityResultContracts.CaptureVideo() */ }
    private fun launchGalleryPicker()  { openGalleryScreen() }
    private fun launchDocumentPicker() { /* TODO: ActivityResultContracts.OpenDocument() */ }
    private fun launchAllFilePicker()  { openBottomSheet() }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun setIconColors(b: ItemPickerOptionBinding, bgRes: Int, tintRes: Int) {
        b.ivOptionIcon.backgroundTintList = ColorStateList.valueOf(color(bgRes))
        b.ivOptionIcon.imageTintList      = ColorStateList.valueOf(color(tintRes))
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)
}