package com.example

import com.example.data.metadata.Cr3BoxParser
import com.example.data.metadata.XmpRatingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class Cr3BoxParserTest {

    private val ADOBE_UUID = byteArrayOf(
        0xBE.toByte(), 0x7A.toByte(), 0xCF.toByte(), 0xCB.toByte(),
        0x97.toByte(), 0xA9.toByte(), 0x42.toByte(), 0xE8.toByte(),
        0x9C.toByte(), 0x71.toByte(), 0x99.toByte(), 0x94.toByte(),
        0x91.toByte(), 0xE3.toByte(), 0xAF.toByte(), 0xAC.toByte()
    )

    @Test
    fun findXmp_inSyntheticCr3Stream_findsAndExtractsXml() {
        val out = ByteArrayOutputStream()

        // 1. Caja 'ftyp' simulada
        val ftypPayload = "crx \u0000\u0000\u0000\u0001".toByteArray(StandardCharsets.US_ASCII)
        val ftypSize = 8 + ftypPayload.size
        writeInt(out, ftypSize)
        out.write("ftyp".toByteArray(StandardCharsets.US_ASCII))
        out.write(ftypPayload)

        // 2. Caja 'uuid' con Adobe XMP UUID
        val xmpString = "<x:xmpmeta><xmp:Rating>5</xmp:Rating></x:xmpmeta>"
        val xmpBytes = xmpString.toByteArray(StandardCharsets.UTF_8)
        val uuidBoxSize = 8 + 16 + xmpBytes.size
        writeInt(out, uuidBoxSize)
        out.write("uuid".toByteArray(StandardCharsets.US_ASCII))
        out.write(ADOBE_UUID)
        out.write(xmpBytes)

        val input = ByteArrayInputStream(out.toByteArray())
        val foundXmp = Cr3BoxParser.findXmp(input)

        assertNotNull(foundXmp)
        assertEquals(xmpString, foundXmp)

        val rating = XmpRatingParser.parseRating(foundXmp!!)
        assertEquals(5, rating)
    }

    @Test
    fun findXmp_whenNoXmpUuidPresent_returnsNull() {
        val out = ByteArrayOutputStream()

        // Caja ftyp
        writeInt(out, 16)
        out.write("ftyp".toByteArray(StandardCharsets.US_ASCII))
        out.write(ByteArray(8))

        // Caja moov sin uuid
        writeInt(out, 16)
        out.write("moov".toByteArray(StandardCharsets.US_ASCII))
        out.write(ByteArray(8))

        val input = ByteArrayInputStream(out.toByteArray())
        val foundXmp = Cr3BoxParser.findXmp(input)

        assertNull(foundXmp)
    }

    private fun writeInt(out: ByteArrayOutputStream, v: Int) {
        out.write((v ushr 24) and 0xFF)
        out.write((v ushr 16) and 0xFF)
        out.write((v ushr 8) and 0xFF)
        out.write(v and 0xFF)
    }
}
