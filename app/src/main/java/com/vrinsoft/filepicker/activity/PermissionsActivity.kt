package com.vrinsoft.filepicker.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.adapter.ResultFilesAdapter
import com.vrinsoft.filepicker.databinding.ActivityPermissionsBinding
import com.vrinsoft.filepicker.databinding.ItemPermissionRowBinding
import com.vrinsoft.filepicker.databinding.LayoutPermissionRationaleBinding
import com.vrinsoft.filepicker.model.ResultFileItem

/**
 * PermissionsActivity — Screen 4
 * Shows runtime permission toggles and the list of picked file results.
 */
class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var rationaleBinding: LayoutPermissionRationaleBinding

    // ── Permission definitions ────────────────────────────────────────────

    private val permissionsToRequest: Array<String> get() = buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshPermissionRows()
        }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rationaleBinding = LayoutPermissionRationaleBinding.bind(binding.rationaleCard.root)

        setupToolbar()
        setupPermissionRows()
        setupRationaleCard()
        setupResultList()
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from system Settings
        refreshPermissionRows()
    }

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Permission rows ──────────────────────────────────────────────────

    private fun setupPermissionRows() {
        // Camera
        ItemPermissionRowBinding.bind(binding.permCamera.root).apply {
            cvPermIconBg.setCardBackgroundColor(color(R.color.icon_bg_blue))
            ivPermIcon.setImageResource(R.drawable.ic_camera)
            ivPermIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_blue))
            tvPermName.setText(R.string.perm_camera)
            tvPermDesc.setText(R.string.perm_camera_desc)
            switchPerm.setOnCheckedChangeListener(null)
            switchPerm.setOnClickListener {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }

        // Media Library
        ItemPermissionRowBinding.bind(binding.permMedia.root).apply {
            cvPermIconBg.setCardBackgroundColor(color(R.color.icon_bg_teal))
            ivPermIcon.setImageResource(R.drawable.ic_photo)
            ivPermIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_teal))
            tvPermName.setText(R.string.perm_media)
            tvPermDesc.setText(R.string.perm_media_desc)
            switchPerm.setOnCheckedChangeListener(null)
            switchPerm.setOnClickListener {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                else
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissionsLauncher.launch(perms)
            }
        }

        // Storage Read
        ItemPermissionRowBinding.bind(binding.permStorage.root).apply {
            cvPermIconBg.setCardBackgroundColor(color(R.color.icon_bg_amber))
            ivPermIcon.setImageResource(R.drawable.ic_file_text)
            ivPermIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_amber))
            tvPermName.setText(R.string.perm_storage)
            tvPermDesc.setText(R.string.perm_storage_desc)
            switchPerm.setOnCheckedChangeListener(null)
            switchPerm.setOnClickListener {
                requestPermissionsLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
            }
        }

        refreshPermissionRows()
    }

    private fun refreshPermissionRows() {
        val cameraGranted  = isGranted(Manifest.permission.CAMERA)
        val mediaGranted   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            isGranted(Manifest.permission.READ_MEDIA_IMAGES)
        else
            isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        val storageGranted = isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)

        applyPermissionState(binding.permCamera.root,  cameraGranted)
        applyPermissionState(binding.permMedia.root,   mediaGranted)
        applyPermissionState(binding.permStorage.root, storageGranted)

        // Show rationale card if any permission is denied
        val anyDenied = !cameraGranted || !mediaGranted || !storageGranted
        binding.rationaleCard.root.visibility = if (anyDenied) View.VISIBLE else View.GONE
    }

    private fun applyPermissionState(rowView: View, granted: Boolean) {
        ItemPermissionRowBinding.bind(rowView).apply {
            switchPerm.isChecked = granted
            tvPermStatus.text = getString(
                if (granted) R.string.perm_status_granted else R.string.perm_status_denied
            )
            tvPermStatus.setTextColor(color(
                if (granted) R.color.badge_text_new else R.color.badge_text_required
            ))
            tvPermStatus.backgroundTintList = ColorStateList.valueOf(color(
                if (granted) R.color.badge_bg_new else R.color.badge_bg_required
            ))
        }
    }

    // ── Rationale card ───────────────────────────────────────────────────

    private fun setupRationaleCard() {
        rationaleBinding.btnGoToSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }
    }

    @Suppress("DEPRECATION")
    private fun getPickedFiles(): ArrayList<ResultFileItem> {
        val data = intent.getSerializableExtra(KEY_PICKED_FILES)

        return if (data is ArrayList<*>) {
            ArrayList(
                data.filterIsInstance<ResultFileItem>()
            )
        } else {
            arrayListOf()
        }
    }

    // ── Result file list ─────────────────────────────────────────────────
    private fun setupResultList() {
        binding.rvResultFiles.layoutManager = LinearLayoutManager(this)

        // Retrieve picked files passed from GalleryActivity / other pickers
        val pickedFiles = getPickedFiles()

        if (pickedFiles.isEmpty()) {
            binding.emptyState.root.visibility  = View.VISIBLE
            binding.rvResultFiles.visibility = View.GONE
        } else {
            binding.emptyState.root.visibility  = View.GONE
            binding.rvResultFiles.visibility = View.VISIBLE
            binding.rvResultFiles.adapter = ResultFilesAdapter(pickedFiles)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun color(res: Int) = ContextCompat.getColor(this, res)

    companion object {
        const val KEY_PICKED_FILES = "key_picked_files"
    }
}