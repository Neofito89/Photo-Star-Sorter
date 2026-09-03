package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.PhotoCacheDao
import com.example.data.db.PhotoCacheEntity
import com.example.data.metadata.DefaultMetadataExtractor
import com.example.data.metadata.MetadataExtractor
import com.example.data.model.PhotoItem
import com.example.data.model.RatingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class OperationResult(
    val successCount: Int,
    val failedCount: Int,
    val errorMessages: List<String> = emptyList()
)

class PhotoRepository(
    private val context: Context,
    private val cacheDao: PhotoCacheDao,
    private val metadataExtractor: MetadataExtractor = DefaultMetadataExtractor()
) {

    companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "JPG", "JPEG", "CR2", "CR3", "HEIC", "HEIF", "TIF", "TIFF", "DNG"
        )
    }

    /**
     * Escanea recursivamente el árbol de archivos sin cargar los contenidos en memoria.
     * Emite lotes progresivos de fotografías encontradas para mantener la interfaz fluida.
     */
    fun scanDirectory(
        treeUri: Uri,
        onStatusUpdate: (count: Int, currentName: String) -> Unit
    ): Flow<List<PhotoItem>> = flow {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.canRead()) {
            emit(emptyList())
            return@flow
        }

        val treeUriStr = treeUri.toString()
        val allDiscovered = mutableListOf<PhotoItem>()
        var lastEmitTime = System.currentTimeMillis()

        suspend fun scanRecursive(folder: DocumentFile, currentPath: String) {
            val files = try {
                folder.listFiles()
            } catch (e: Exception) {
                Log.w("PhotoRepository", "No se pudo listar $currentPath: ${e.message}")
                emptyArray()
            }

            for (file in files) {
                if (file.isDirectory) {
                    val subPath = if (currentPath.isEmpty()) file.name ?: "" else "$currentPath/${file.name ?: ""}"
                    scanRecursive(file, subPath)
                } else if (file.isFile) {
                    val name = file.name ?: continue
                    val ext = name.substringAfterLast('.', "").uppercase(Locale.US)
                    if (SUPPORTED_EXTENSIONS.contains(ext)) {
                        val fileSize = file.length()
                        val lastModified = file.lastModified()
                        val uriStr = file.uri.toString()

                        // 1. Revisar caché de base de datos Room
                        val cached = cacheDao.getValidCached(uriStr, lastModified, fileSize)
                        val photoItem: PhotoItem

                        if (cached != null) {
                            val status = try {
                                RatingStatus.valueOf(cached.ratingStatus)
                            } catch (_: Exception) {
                                RatingStatus.UNAVAILABLE
                            }

                            photoItem = PhotoItem(
                                uri = file.uri,
                                uriString = uriStr,
                                fileName = name,
                                relativePath = currentPath,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                mimeType = cached.mimeType,
                                rating = cached.rating,
                                ratingStatus = status,
                                captureDate = cached.captureDate,
                                captureDateFormatted = cached.captureDateFormatted,
                                cameraModel = cached.cameraModel
                            )
                        } else {
                            // 2. Extraer metadatos con el extractor modular
                            val mime = file.type ?: "image/*"
                            val extracted = metadataExtractor.extract(context, file.uri, name, mime)

                            photoItem = PhotoItem(
                                uri = file.uri,
                                uriString = uriStr,
                                fileName = name,
                                relativePath = currentPath,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                mimeType = mime,
                                rating = extracted.rating,
                                ratingStatus = extracted.ratingStatus,
                                captureDate = extracted.captureDate,
                                captureDateFormatted = extracted.captureDateFormatted,
                                cameraModel = extracted.cameraModel
                            )

                            // Guardar en caché Room
                            val entity = PhotoCacheEntity(
                                uriString = uriStr,
                                treeUriString = treeUriStr,
                                fileName = name,
                                relativePath = currentPath,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                mimeType = mime,
                                rating = extracted.rating,
                                ratingStatus = extracted.ratingStatus.name,
                                captureDate = extracted.captureDate,
                                captureDateFormatted = extracted.captureDateFormatted,
                                cameraModel = extracted.cameraModel,
                                extraDetails = extracted.extraDetails
                            )
                            cacheDao.insertOrUpdate(entity)
                        }

                        allDiscovered.add(photoItem)
                        onStatusUpdate(allDiscovered.size, name)

                        // Emisión progresiva cada 25 fotos o cada 400ms para alta respuesta visual
                        val now = System.currentTimeMillis()
                        if (allDiscovered.size % 25 == 0 || (now - lastEmitTime > 400)) {
                            emit(allDiscovered.toList())
                            lastEmitTime = now
                        }
                    }
                }
            }
        }

        scanRecursive(rootDoc, "")
        emit(allDiscovered.toList())
    }.flowOn(Dispatchers.IO)

    /**
     * Copia los archivos seleccionados a la carpeta de destino SAF.
     * En caso de colisión de nombres, renombra con sufijo "(1)", "(2)" para evitar sobreescritura.
     */
    suspend fun copyPhotos(
        photos: List<PhotoItem>,
        destinationTreeUri: Uri,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit
    ): OperationResult = withContext(Dispatchers.IO) {
        val destFolder = DocumentFile.fromTreeUri(context, destinationTreeUri)
            ?: return@withContext OperationResult(
                0,
                photos.size,
                listOf("No se pudo acceder a la carpeta de destino.")
            )

        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        val existingFiles = mutableSetOf<String>()
        try {
            destFolder.listFiles().forEach { f ->
                f.name?.let { existingFiles.add(it.lowercase(Locale.ROOT)) }
            }
        } catch (_: Exception) {}

        photos.forEachIndexed { index, item ->
            onProgress(index + 1, photos.size, item.fileName)
            try {
                val targetFileName = generateSafeFileName(item.fileName, existingFiles)
                val targetMime = if (item.mimeType.isNotBlank()) item.mimeType else "image/*"
                val targetFile = destFolder.createFile(targetMime, targetFileName)
                    ?: throw IOException("No se pudo crear el archivo destino para ${item.fileName}")

                context.contentResolver.openInputStream(item.uri)?.use { inStream ->
                    context.contentResolver.openOutputStream(targetFile.uri)?.use { outStream ->
                        inStream.copyTo(outStream, bufferSize = 64 * 1024)
                    } ?: throw IOException("Error abriendo flujo de escritura destino")
                } ?: throw IOException("Error abriendo archivo de origen")

                existingFiles.add(targetFileName.lowercase(Locale.ROOT))
                successCount++
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Error copiando ${item.fileName}: ${e.message}")
                failedCount++
                errors.add("${item.fileName}: ${e.localizedMessage ?: "Error desconocido"}")
            }
        }

        OperationResult(successCount, failedCount, errors)
    }

    /**
     * Elimina los archivos seleccionados de la tarjeta SD usando los permisos SAF.
     */
    suspend fun deletePhotos(
        photos: List<PhotoItem>,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit
    ): OperationResult = withContext(Dispatchers.IO) {
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        photos.forEachIndexed { index, item ->
            onProgress(index + 1, photos.size, item.fileName)
            try {
                val doc = DocumentFile.fromSingleUri(context, item.uri)
                val deleted = doc?.delete() ?: false
                if (deleted) {
                    successCount++
                    cacheDao.deleteByUri(item.uriString)
                } else {
                    failedCount++
                    errors.add("${item.fileName}: El archivo no pudo ser eliminado del soporte USB.")
                }
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Error eliminando ${item.fileName}: ${e.message}")
                failedCount++
                errors.add("${item.fileName}: ${e.localizedMessage ?: "Error de permisos"}")
            }
        }

        OperationResult(successCount, failedCount, errors)
    }

    suspend fun clearCacheForTree(treeUri: Uri) = withContext(Dispatchers.IO) {
        cacheDao.clearForTree(treeUri.toString())
    }

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        cacheDao.clearAll()
    }

    private fun generateSafeFileName(originalName: String, existingNames: Set<String>): String {
        val lowerOriginal = originalName.lowercase(Locale.ROOT)
        if (!existingNames.contains(lowerOriginal)) {
            return originalName
        }

        val dotIndex = originalName.lastIndexOf('.')
        val namePart = if (dotIndex != -1) originalName.substring(0, dotIndex) else originalName
        val extPart = if (dotIndex != -1) originalName.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidate = "$namePart ($counter)$extPart"
            if (!existingNames.contains(candidate.lowercase(Locale.ROOT))) {
                return candidate
            }
            counter++
        }
    }
}
