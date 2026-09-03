package com.example.data.metadata

import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Analizador de cajas ISOBMFF para archivos Canon RAW (.CR3).
 *
 * Los archivos CR3 son contenedores ISO-BMFF (similares a MP4).
 * Canon incluye paquetes de metadatos Adobe XMP dentro de cajas 'uuid' con el
 * identificador estándar de Adobe:
 * BE 7A CF CB 97 A9 42 E8 9C 71 99 94 91 E3 AF AC.
 */
object Cr3BoxParser {

    private val XMP_UUID = byteArrayOf(
        0xBE.toByte(), 0x7A.toByte(), 0xCF.toByte(), 0xCB.toByte(),
        0x97.toByte(), 0xA9.toByte(), 0x42.toByte(), 0xE8.toByte(),
        0x9C.toByte(), 0x71.toByte(), 0x99.toByte(), 0x94.toByte(),
        0x91.toByte(), 0xE3.toByte(), 0xAF.toByte(), 0xAC.toByte()
    )

    /**
     * Escanea un stream de archivo CR3 en busca de la caja XMP uuid.
     * Retorna la cadena XML XMP si se encuentra de forma fiable, o null en caso contrario.
     */
    fun findXmp(inputStream: InputStream, maxBytesToScan: Long = 10L * 1024 * 1024): String? {
        val header = ByteArray(8)
        var bytesReadTotal = 0L

        while (bytesReadTotal + 8 <= maxBytesToScan) {
            val read = readFully(inputStream, header, 8)
            if (read < 8) break
            bytesReadTotal += 8

            val boxSize = readInt(header, 0).toLong() and 0xFFFFFFFFL
            val boxType = String(header, 4, 4, StandardCharsets.US_ASCII)

            val (actualSize, headerSize) = when (boxSize) {
                1L -> {
                    // Tamaño extendido de 64 bits
                    val largeSizeBuf = ByteArray(8)
                    val lr = readFully(inputStream, largeSizeBuf, 8)
                    if (lr < 8) break
                    bytesReadTotal += 8
                    val largeSize = readLong(largeSizeBuf, 0)
                    Pair(largeSize, 16L)
                }
                0L -> {
                    // La caja se extiende hasta el final
                    Pair(maxBytesToScan - bytesReadTotal + 8, 8L)
                }
                else -> {
                    Pair(boxSize, 8L)
                }
            }

            if (actualSize < headerSize) break

            val payloadSize = actualSize - headerSize

            if (boxType == "uuid") {
                if (payloadSize >= 16) {
                    val uuidBuf = ByteArray(16)
                    val ur = readFully(inputStream, uuidBuf, 16)
                    if (ur == 16) {
                        bytesReadTotal += 16
                        if (uuidBuf.contentEquals(XMP_UUID)) {
                            // Encontramos la caja XMP de Adobe
                            val xmpPayloadSize = (payloadSize - 16).toInt().coerceAtMost(1024 * 1024)
                            if (xmpPayloadSize > 0) {
                                val xmpData = ByteArray(xmpPayloadSize)
                                val xr = readFully(inputStream, xmpData, xmpPayloadSize)
                                if (xr > 0) {
                                    return String(xmpData, 0, xr, StandardCharsets.UTF_8)
                                }
                            }
                        } else {
                            // Saltar el resto del payload de uuid no relevante
                            skipBytes(inputStream, payloadSize - 16)
                            bytesReadTotal += (payloadSize - 16)
                        }
                    }
                } else {
                    skipBytes(inputStream, payloadSize)
                    bytesReadTotal += payloadSize
                }
            } else if (boxType == "moov" || boxType == "trak" || boxType == "mdia") {
                // Caja contenedora: continuar leyendo dentro de ella secuencialmente
                continue
            } else {
                // Saltar caja no relevante
                val skipped = skipBytes(inputStream, payloadSize)
                bytesReadTotal += skipped
                if (skipped < payloadSize) break
            }
        }

        return null
    }

    private fun readFully(inputStream: InputStream, b: ByteArray, len: Int): Int {
        var n = 0
        while (n < len) {
            val count = inputStream.read(b, n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }

    private fun skipBytes(inputStream: InputStream, toSkip: Long): Long {
        var remaining = toSkip
        val buf = ByteArray(4096)
        while (remaining > 0) {
            val count = inputStream.read(buf, 0, remaining.coerceAtMost(buf.size.toLong()).toInt())
            if (count < 0) break
            remaining -= count
        }
        return toSkip - remaining
    }

    private fun readInt(b: ByteArray, offset: Int): Int {
        return ((b[offset].toInt() and 0xFF) shl 24) or
                ((b[offset + 1].toInt() and 0xFF) shl 16) or
                ((b[offset + 2].toInt() and 0xFF) shl 8) or
                (b[offset + 3].toInt() and 0xFF)
    }

    private fun readLong(b: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = (result shl 8) or (b[offset + i].toLong() and 0xFFL)
        }
        return result
    }
}
