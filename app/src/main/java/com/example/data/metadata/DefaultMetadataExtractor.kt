package com.example.data.metadata

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.RatingStatus
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DefaultMetadataExtractor : MetadataExtractor {

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val exifDateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    override fun extract(
        context: Context,
        uri: Uri,
        fileName: String,
        mimeType: String?
    ): ExtractedPhotoMetadata {
        val ext = fileName.substringAfterLast('.', "").uppercase(Locale.US)

        return try {
            when (ext) {
                "CR3" -> extractCr3(context, uri)
                "CR2" -> extractCr2(context, uri)
                else -> extractStandardImage(context, uri, ext)
            }
        } catch (e: SecurityException) {
            Log.w("MetadataExtractor", "Permiso denegado para $uri: ${e.message}")
            ExtractedPhotoMetadata(
                rating = null,
                ratingStatus = RatingStatus.ERROR,
                extraDetails = "Permiso revocado"
            )
        } catch (e: Exception) {
            Log.w("MetadataExtractor", "Error leyendo metadatos de $fileName: ${e.message}")
            ExtractedPhotoMetadata(
                rating = null,
                ratingStatus = RatingStatus.UNAVAILABLE,
                extraDetails = e.localizedMessage ?: "Error de lectura"
            )
        }
    }

    private fun extractCr3(context: Context, uri: Uri): ExtractedPhotoMetadata {
        var xmpString: String? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                xmpString = Cr3BoxParser.findXmp(stream)
            }
        } catch (e: Exception) {
            Log.w("DefaultMetadataExtractor", "Fallo al escanear cajas CR3: ${e.message}")
        }

        if (!xmpString.isNullOrBlank()) {
            val rating = XmpRatingParser.parseRating(xmpString!!)
            val captureDate = XmpRatingParser.parseCaptureDate(xmpString!!)
            val dateFormatted = captureDate?.let { displayDateFormat.format(Date(it)) }

            val status = when {
                rating != null && rating in 1..5 -> RatingStatus.RATED
                rating == 0 -> RatingStatus.UNRATED
                else -> RatingStatus.UNAVAILABLE
            }

            return ExtractedPhotoMetadata(
                rating = rating,
                ratingStatus = status,
                captureDate = captureDate,
                captureDateFormatted = dateFormatted,
                cameraModel = "Canon EOS (CR3)",
                extraDetails = "XMP embebido en caja ISOBMFF"
            )
        }

        // Si no pudimos leer el XMP en CR3 de forma fiable, retornamos fallback elegante
        return ExtractedPhotoMetadata(
            rating = null,
            ratingStatus = RatingStatus.UNAVAILABLE,
            captureDate = null,
            captureDateFormatted = null,
            cameraModel = "Canon EOS RAW (CR3)",
            extraDetails = "Rating no disponible en RAW"
        )
    }

    private fun extractCr2(context: Context, uri: Uri): ExtractedPhotoMetadata {
        var rating: Int? = null
        var captureDate: Long? = null
        var cameraModel: String? = null

        // 1. Intentar con ExifInterface de AndroidX (soporta encabezados TIFF de CR2)
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (!dateStr.isNullOrBlank()) {
                    try {
                        captureDate = exifDateFormat.parse(dateStr)?.time
                    } catch (_: Exception) {}
                }

                // Verificar atributo de calificación si existe
                val rawRating = exif.getAttribute("Rating")
                val intRating = rawRating?.toIntOrNull()
                if (intRating != null && intRating in 0..5) {
                    rating = intRating
                }
            }
        } catch (e: Exception) {
            Log.d("DefaultMetadataExtractor", "Exif CR2 parcial: ${e.message}")
        }

        // 2. Priorizar XMP extrayendo el paquete en el encabezado de CR2
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val xmp = XmpRatingParser.extractXmpFromStream(stream, maxBytesToRead = 1024 * 512)
                if (xmp != null) {
                    val xmpRating = XmpRatingParser.parseRating(xmp)
                    if (xmpRating != null) {
                        rating = xmpRating
                    }
                    if (captureDate == null) {
                        captureDate = XmpRatingParser.parseCaptureDate(xmp)
                    }
                }
            }
        } catch (_: Exception) {}

        val status = when {
            rating != null && rating in 1..5 -> RatingStatus.RATED
            rating == 0 -> RatingStatus.UNRATED
            rating == null -> RatingStatus.UNAVAILABLE
            else -> RatingStatus.UNAVAILABLE
        }

        val dateFormatted = captureDate?.let { displayDateFormat.format(Date(it)) }

        return ExtractedPhotoMetadata(
            rating = rating,
            ratingStatus = status,
            captureDate = captureDate,
            captureDateFormatted = dateFormatted,
            cameraModel = cameraModel ?: "Canon EOS (CR2)",
            extraDetails = if (rating != null) "XMP/Exif CR2" else "Rating no disponible en RAW"
        )
    }

    private fun extractStandardImage(context: Context, uri: Uri, ext: String): ExtractedPhotoMetadata {
        var rating: Int? = null
        var captureDate: Long? = null
        var cameraModel: String? = null

        // 1. Leer EXIF básico
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (!dateStr.isNullOrBlank()) {
                    try {
                        captureDate = exifDateFormat.parse(dateStr)?.time
                    } catch (_: Exception) {}
                }

                val exifRating = exif.getAttribute("Rating")
                val intRating = exifRating?.toIntOrNull()
                if (intRating != null && intRating in 0..5) {
                    rating = intRating
                }
            }
        } catch (_: Exception) {}

        // 2. Priorizar XMP escaneando el flujo APP1
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val xmp = XmpRatingParser.extractXmpFromStream(stream, maxBytesToRead = 1024 * 256)
                if (xmp != null) {
                    val xmpRating = XmpRatingParser.parseRating(xmp)
                    if (xmpRating != null) {
                        rating = xmpRating
                    }
                    if (captureDate == null) {
                        captureDate = XmpRatingParser.parseCaptureDate(xmp)
                    }
                }
            }
        } catch (_: Exception) {}

        val status = when {
            rating != null && rating in 1..5 -> RatingStatus.RATED
            rating == 0 -> RatingStatus.UNRATED
            else -> {
                // En imágenes estándar (JPEG, HEIC, TIFF), si no hay calificación explícita,
                // representa una foto no calificada (0 estrellas)
                rating = 0
                RatingStatus.UNRATED
            }
        }

        val dateFormatted = captureDate?.let { displayDateFormat.format(Date(it)) }

        return ExtractedPhotoMetadata(
            rating = rating,
            ratingStatus = status,
            captureDate = captureDate,
            captureDateFormatted = dateFormatted,
            cameraModel = cameraModel,
            extraDetails = ext
        )
    }
}
