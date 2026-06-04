package com.vrinsoft.filepicker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.vrinsoft.filepicker.databinding.ActivityMainBinding
import com.vrinsoft.filepicker.databinding.ItemPickerOptionBinding
import com.vrinsoft.filepicker.databinding.ItemQuickActionBinding

/**
 * MainActivity.kt
 *
 * Wires up activity_home.xml and all its included layouts.
 * Replace the TODO placeholders with your actual library launch calls.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var qaAllFilesBinding: ItemQuickActionBinding
    private lateinit var qaSettingsBinding: ItemQuickActionBinding
    private lateinit var qaRecentBinding: ItemQuickActionBinding
    private lateinit var qaPermissionsBinding: ItemQuickActionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        qaAllFilesBinding = ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaAllFiles.root)
        qaSettingsBinding = ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaSettings.root)
        qaRecentBinding = ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaRecent.root)
        qaPermissionsBinding = ItemQuickActionBinding.bind(binding.layoutQuickActionsGrid.qaPermissions.root)

        setupPickerOptions()
        setupQuickActions()
        setupCta()
        setupBottomNav()
    }

    // ── Picker option rows ──────────────────────────────────────────────

    private fun setupPickerOptions() {
        // Image capture row
        ItemPickerOptionBinding.bind(binding.rowImageCapture.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_camera)
            setIconColors(this, R.color.icon_bg_blue, R.color.icon_tint_blue)
            tvOptionName.setText(R.string.option_camera)
            tvOptionDesc.setText(R.string.option_camera_desc)
            tvBadge.visibility = View.GONE
            root.setOnClickListener { launchImageCapture() }
        }

        // Video capture row
        ItemPickerOptionBinding.bind(binding.rowVideoCapture.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_video)
            setIconColors(this, R.color.icon_bg_coral, R.color.icon_tint_coral)
            tvOptionName.setText(R.string.option_video)
            tvOptionDesc.setText(R.string.option_video_desc)
            tvBadge.visibility = View.GONE
            root.setOnClickListener { launchVideoCapture() }
        }

        // Gallery row — with "Multi" badge
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

        // Documents row
        ItemPickerOptionBinding.bind(binding.rowDocuments.root).apply {
            ivOptionIcon.setImageResource(R.drawable.ic_file_text)
            setIconColors(this, R.color.icon_bg_amber, R.color.icon_tint_amber)
            tvOptionName.setText(R.string.option_documents)
            tvOptionDesc.setText(R.string.option_documents_desc)
            tvBadge.visibility = View.GONE
            root.setOnClickListener { launchDocumentPicker() }
        }
    }

    // ── Quick action cards ──────────────────────────────────────────────

    private fun setupQuickActions() {
        // All files
        qaAllFilesBinding.apply {
            ivQaIcon.setImageResource(R.drawable.ic_folder_open)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_purple))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_purple))
            tvQaLabel.setText(R.string.qa_all_files)
            tvQaDesc.setText(R.string.qa_all_files_desc)
            root.setOnClickListener { launchAllFilePicker() }
        }

        // Settings
        qaSettingsBinding.apply {
            ivQaIcon.setImageResource(R.drawable.ic_settings)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_blue))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_blue))
            tvQaLabel.setText(R.string.qa_settings)
            tvQaDesc.setText(R.string.qa_settings_desc)
            root.setOnClickListener { openSettings() }
        }

        // Recent
        qaRecentBinding.apply {
            ivQaIcon.setImageResource(R.drawable.ic_history)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_green))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_green))
            tvQaLabel.setText(R.string.qa_recent)
            tvQaDesc.setText(R.string.qa_recent_desc)
            root.setOnClickListener { openRecent() }
        }

        // Permissions
        qaPermissionsBinding.apply {
            ivQaIcon.setImageResource(R.drawable.ic_shield_check)
            cvQaIconBg.setCardBackgroundColor(color(R.color.qa_bg_amber))
            ivQaIcon.imageTintList = ColorStateList.valueOf(color(R.color.qa_tint_amber))
            tvQaLabel.setText(R.string.qa_permissions)
            tvQaDesc.setText(R.string.qa_permissions_desc)
            root.setOnClickListener { openPermissions() }
        }
    }

    // ── CTA Button ──────────────────────────────────────────────────────

    private fun setupCta() {
        binding.btnOpenPicker.setOnClickListener {
            launchAllFilePicker()
        }
    }

    // ── Bottom navigation ───────────────────────────────────────────────

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> true
                R.id.nav_files   -> { openFilesTab(); true }
                R.id.nav_recent  -> { openRecent(); true }
                else             -> false
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun setIconColors(
        binding: ItemPickerOptionBinding,
        bgColorRes: Int,
        tintColorRes: Int
    ) {
        binding.ivOptionIcon.backgroundTintList =
            ColorStateList.valueOf(color(bgColorRes))
        binding.ivOptionIcon.imageTintList =
            ColorStateList.valueOf(color(tintColorRes))
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)

    // ── Library launch stubs (replace with your actual contracts) ───────

    private fun launchImageCapture()  { /* TODO: registerForActivityResult(ImageCapture()) */ }
    private fun launchVideoCapture()  { /* TODO: registerForActivityResult(VideoCapture()) */ }
    private fun launchGalleryPicker() { /* TODO: registerForActivityResult(PickMedia()) */ }
    private fun launchDocumentPicker(){ /* TODO: registerForActivityResult(PickDocumentFile()) */ }
    private fun launchAllFilePicker() { /* TODO: registerForActivityResult(AllFilePicker()) */ }
    private fun openSettings()        { /* TODO: start SettingsActivity */ }
    private fun openRecent()          { /* TODO: navigate to RecentFragment */ }
    private fun openPermissions()     { /* TODO: start PermissionsActivity */ }
    private fun openFilesTab()        { /* TODO: navigate to FilesFragment */ }
}
