package com.vrinsoft.filepicker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResultFileItem(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uriString: String,
    val durationMs: Long = 0L,
    val dateAdded: Long  = 0L
) : Parcelable {

    val formattedSize: String get() {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.0f KB".format(kb)
            else      -> "$sizeBytes B"
        }
    }

    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isDocument: Boolean get() = !isVideo && !isImage
}