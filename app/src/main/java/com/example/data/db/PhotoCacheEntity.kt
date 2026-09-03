package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_cache",
    indices = [
        Index(value = ["treeUriString"]),
        Index(value = ["uriString", "lastModified", "fileSize"])
    ]
)
data class PhotoCacheEntity(
    @PrimaryKey
    val uriString: String,
    val treeUriString: String,
    val fileName: String,
    val relativePath: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val rating: Int?,
    val ratingStatus: String,
    val captureDate: Long?,
    val captureDateFormatted: String?,
    val cameraModel: String?,
    val extraDetails: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
