package com.vrinsoft.filepicker.activity

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vrinsoft.filepicker.model.ResultFileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsulates all picker logic extracted from MainActivity.
 * Register this in your Activity before onCreate completes.
 */
class FilePickerDelegate(
    private val activity: ComponentActivity,
    private val maxSelection: Int = Int.MAX_VALUE,
    private val onResult: (List<ResultFileItem>) -> Unit
) {

    private var pendingCameraUri: Uri? = null

    // ── Launchers ────────────────────────────────────────────────────────

    private val cameraPermLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startCamera()
            else Toast.makeText(activity, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val storagePermLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            if (perms.values.all { it }) launchGalleryScreen()
            else Toast.makeText(activity, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }

    private val takePictureLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                pendingCameraUri?.let { uri ->
                    deliverResult(arrayListOf(
                        ResultFileItem(
                            displayName = "captured_photo.jpg",
                            mimeType    = "image/jpeg",
                            sizeBytes   = 0L,
                            uriString   = uri.toString()
                        )
                    ))
                }
            } else {
                Toast.makeText(activity, "Capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickSingleDocumentLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri ?: return@registerForActivityResult
            activity.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            deliverResult(arrayListOf(uriToResultFile(uri)))
        }

    private val pickMultipleDocumentsLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            if (uris.isEmpty()) return@registerForActivityResult
            uris.forEach { uri ->
                activity.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            deliverResult(ArrayList(uris.map { uriToResultFile(it) }))
        }

    private val galleryLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == ComponentActivity.RESULT_OK) {
                val files: ArrayList<ResultFileItem>? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        result.data?.getParcelableArrayListExtra(
                            GalleryActivity.KEY_RESULT_FILES, ResultFileItem::class.java)
                    else
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableArrayListExtra(GalleryActivity.KEY_RESULT_FILES)

                if (!files.isNullOrEmpty()) deliverResult(files)
            }
        }

    // ── Public entry points ───────────────────────────────────────────────

    fun launchCamera() {
        if (hasPerm(Manifest.permission.CAMERA)) startCamera()
        else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchVideo() {
        // Reuse gallery filtered to videos
        launchGallery()
    }

    fun launchGallery() {
        if (hasStoragePermission()) launchGalleryScreen()
        else storagePermLauncher.launch(storagePermissions())
    }

    fun launchDocument(multiple: Boolean = false) {
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
        if (multiple) pickMultipleDocumentsLauncher.launch(mimeTypes)
        else pickSingleDocumentLauncher.launch(mimeTypes)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun launchGalleryScreen() {
        val intent = Intent(activity, GalleryActivity::class.java).apply {
            putExtra(GalleryActivity.KEY_MAX_SELECTION, maxSelection)
        }
        galleryLauncher.launch(intent)
    }

//    private fun deliverResult(files: ArrayList<ResultFileItem>) {
//        // Go to ResultsActivity, which will call setResult back to caller
//        val intent = Intent(activity, ResultsActivity::class.java).apply {
//            putParcelableArrayListExtra(ResultsActivity.KEY_SELECTED_FILES, files)
//        }
//        activity.startActivity(intent)
//    }

    private fun deliverResult(files: ArrayList<ResultFileItem>) {
        onResult(files)  // directly callback — no ResultsActivity middleman
    }

    private fun startCamera() {
        pendingCameraUri = createImageUri()
        if (pendingCameraUri != null) takePictureLauncher.launch(pendingCameraUri!!)
        else Toast.makeText(activity, "Could not create image file", Toast.LENGTH_SHORT).show()
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
            activity.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FilePicker")
            dir.mkdirs()
            FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.filepicker.provider",
                File(dir, filename)
            )
        }
    }

    private fun uriToResultFile(uri: Uri): ResultFileItem {
        var name = uri.lastPathSegment ?: "document"
        var mime = "application/octet-stream"
        var size = 0L
        activity.contentResolver.query(
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
        if (mime == "application/octet-stream") {
            activity.contentResolver.getType(uri)?.let { mime = it }
        }
        return ResultFileItem(
            displayName = name,
            mimeType    = mime,
            sizeBytes   = size,
            uriString   = uri.toString()
        )
    }

    private fun hasPerm(p: String) =
        ContextCompat.checkSelfPermission(activity, p) == PackageManager.PERMISSION_GRANTED

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
}