package com.example.data.metadata

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * Analizador puro para paquetes y cadenas XMP, enfocado en extraer calificaciones
 * por estrellas (0 a 5) y fechas de captura compatibles con cámaras Canon.
 */
object XmpRatingParser {

    private val RATING_TAG_PATTERN = Pattern.compile(
        "<(?:[a-zA-Z0-9_]+:)?Rating>\\s*([0-5](?:\\.\\d+)?)\\s*</(?:[a-zA-Z0-9_]+:)?Rating>",
        Pattern.CASE_INSENSITIVE
    )

    private val RATING_ATTR_PATTERN = Pattern.compile(
        "(?:[a-zA-Z0-9_]+:)?Rating\\s*=\\s*[\"']([0-5](?:\\.\\d+)?)[\"']",
        Pattern.CASE_INSENSITIVE
    )

    private val RATING_PERCENT_TAG_PATTERN = Pattern.compile(
        "<(?:[a-zA-Z0-9_]+:)?RatingPercent>\\s*(\\d+)\\s*</(?:[a-zA-Z0-9_]+:)?RatingPercent>",
        Pattern.CASE_INSENSITIVE
    )

    private val URGENCY_PATTERN = Pattern.compile(
        "<(?:photoshop:)?Urgency>\\s*([0-8])\\s*</(?:photoshop:)?Urgency>",
        Pattern.CASE_INSENSITIVE
    )

    private val DATE_TAG_PATTERN = Pattern.compile(
        "<(?:(?:xmp:)?CreateDate|(?:photoshop:)?DateCreated|(?:exif:)?DateTimeOriginal)>\\s*([^<]+)\\s*<",
        Pattern.CASE_INSENSITIVE
    )

    private val DATE_FORMATS = arrayOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )

    /**
     * Parsea la calificación por estrellas (0 a 5) a partir de una cadena XML XMP.
     * Retorna null si no se encuentra ninguna calificación en el XMP.
     */
    fun parseRating(xmpContent: String): Int? {
        if (xmpContent.isBlank()) return null

        // 1. Etiqueta <xmp:Rating>X</xmp:Rating> o <Rating>X</Rating>
        val tagMatcher = RATING_TAG_PATTERN.matcher(xmpContent)
        if (tagMatcher.find()) {
            val rawValue = tagMatcher.group(1)
            val floatVal = rawValue?.toFloatOrNull()
            if (floatVal != null) {
                return floatVal.toInt().coerceIn(0, 5)
            }
        }

        // 2. Atributo xmp:Rating="X" o Rating="X"
        val attrMatcher = RATING_ATTR_PATTERN.matcher(xmpContent)
        if (attrMatcher.find()) {
            val rawValue = attrMatcher.group(1)
            val floatVal = rawValue?.toFloatOrNull()
            if (floatVal != null) {
                return floatVal.toInt().coerceIn(0, 5)
            }
        }

        // 3. Porcentaje de calificación <RatingPercent>
        val percentMatcher = RATING_PERCENT_TAG_PATTERN.matcher(xmpContent)
        if (percentMatcher.find()) {
            val percent = percentMatcher.group(1)?.toIntOrNull()
            if (percent != null) {
                return when {
                    percent <= 0 -> 0
                    percent < 25 -> 1
                    percent < 50 -> 2
                    percent < 75 -> 3
                    percent < 99 -> 4
                    else -> 5
                }
            }
        }

        // 4. Fallback: photoshop:Urgency
        val urgencyMatcher = URGENCY_PATTERN.matcher(xmpContent)
        if (urgencyMatcher.find()) {
            val urgency = urgencyMatcher.group(1)?.toIntOrNull()
            if (urgency != null && urgency in 1..5) {
                return urgency
            }
        }

        return null
    }

    /**
     * Extrae la fecha de captura desde la cadena XMP en milisegundos de época.
     */
    fun parseCaptureDate(xmpContent: String): Long? {
        if (xmpContent.isBlank()) return null
        val matcher = DATE_TAG_PATTERN.matcher(xmpContent)
        if (matcher.find()) {
            val rawDate = matcher.group(1)?.trim() ?: return null
            for (format in DATE_FORMATS) {
                try {
                    val parsed = format.parse(rawDate)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
            }
        }
        return null
    }

    /**
     * Escanea los primeros [maxBytesToRead] bytes de un InputStream buscando el bloque XMP.
     * Encontrará paquetes tanto en JPEG (segmento APP1) como en archivos TIFF/CR2.
     */
    fun extractXmpFromStream(inputStream: InputStream, maxBytesToRead: Int = 1024 * 512): String? {
        val buffer = ByteArray(maxBytesToRead)
        var totalRead = 0
        while (totalRead < maxBytesToRead) {
            val bytesRead = inputStream.read(buffer, totalRead, maxBytesToRead - totalRead)
            if (bytesRead == -1) break
            totalRead += bytesRead
        }

        if (totalRead <= 0) return null

        val searchString = String(buffer, 0, totalRead, StandardCharsets.ISO_8859_1)
        val xmpHeaderIndex = searchString.indexOf("http://ns.adobe.com/xap/1.0/")
        if (xmpHeaderIndex == -1) {
            // Intento de búsqueda directa de tags de Rating en el buffer
            if (searchString.contains("Rating")) {
                val startIdx = searchString.indexOf("<x:xmpmeta")
                if (startIdx != -1) {
                    val endIdx = searchString.indexOf("</x:xmpmeta>", startIdx)
                    if (endIdx != -1) {
                        return searchString.substring(startIdx, endIdx + 12)
                    }
                }
            }
            return null
        }

        // Buscar el inicio y fin del paquete XML
        val packetStart = searchString.lastIndexOf("<?xpacket begin", xmpHeaderIndex)
        val metaStart = searchString.lastIndexOf("<x:xmpmeta", xmpHeaderIndex)
        val start = when {
            packetStart != -1 -> packetStart
            metaStart != -1 -> metaStart
            else -> xmpHeaderIndex
        }

        val packetEnd = searchString.indexOf("<?xpacket end", start)
        val metaEnd = searchString.indexOf("</x:xmpmeta>", start)
        val end = when {
            packetEnd != -1 -> packetEnd + 19
            metaEnd != -1 -> metaEnd + 12
            else -> (start + 8192).coerceAtMost(totalRead)
        }

        return try {
            searchString.substring(start, end)
        } catch (_: Exception) {
            null
        }
    }
}
