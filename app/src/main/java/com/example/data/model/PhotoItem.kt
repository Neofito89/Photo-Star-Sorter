package com.example.data.model

import android.net.Uri

/**
 * Representa un archivo fotográfico escaneado en la tarjeta SD.
 */
data class PhotoItem(
    val uri: Uri,
    val uriString: String = uri.toString(),
    val fileName: String,
    val relativePath: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val rating: Int?, // 0..5 o null si no está disponible
    val ratingStatus: RatingStatus,
    val captureDate: Long? = null,
    val captureDateFormatted: String? = null,
    val cameraModel: String? = null,
    val isSelected: Boolean = false
) {
    val fileExtension: String
        get() = fileName.substringAfterLast('.', "").uppercase()

    val formattedSize: String
        get() = when {
            fileSize >= 1024 * 1024 * 1024 -> String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0))
            fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
            fileSize >= 1024 -> String.format("%.1f KB", fileSize / 1024.0)
            else -> "$fileSize B"
        }
}
