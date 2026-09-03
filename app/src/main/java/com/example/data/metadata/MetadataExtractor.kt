package com.example.data.metadata

import android.content.Context
import android.net.Uri
import com.example.data.model.RatingStatus

data class ExtractedPhotoMetadata(
    val rating: Int?,
    val ratingStatus: RatingStatus,
    val captureDate: Long? = null,
    val captureDateFormatted: String? = null,
    val cameraModel: String? = null,
    val extraDetails: String? = null
)

interface MetadataExtractor {
    fun extract(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String?
    ): ExtractedPhotoMetadata
}
