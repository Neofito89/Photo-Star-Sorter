package com.example

import com.example.data.metadata.XmpRatingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class XmpRatingParserTest {

    @Test
    fun parseRating_withXmpRatingTag_returnsCorrectRating() {
        val xmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description xmlns:xmp="http://ns.adobe.com/xap/1.0/">
               <xmp:Rating>5</xmp:Rating>
              </rdf:Description>
             </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

        val rating = XmpRatingParser.parseRating(xmp)
        assertEquals(5, rating)
    }

    @Test
    fun parseRating_withDecimalRatingTag_roundsCorrectly() {
        val xmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:Description xmlns:xmp="http://ns.adobe.com/xap/1.0/">
               <Rating>4.0</Rating>
              </rdf:Description>
            </x:xmpmeta>
        """.trimIndent()

        val rating = XmpRatingParser.parseRating(xmp)
        assertEquals(4, rating)
    }

    @Test
    fun parseRating_withAttribute_returnsRating() {
        val xmp = """
            <rdf:Description rdf:about="" xmlns:xmp="http://ns.adobe.com/xap/1.0/" xmp:Rating="3"/>
        """.trimIndent()

        val rating = XmpRatingParser.parseRating(xmp)
        assertEquals(3, rating)
    }

    @Test
    fun parseRating_withRatingPercent_mapsToStars() {
        val xmp99 = "<RatingPercent>99</RatingPercent>"
        val xmp75 = "<xmp:RatingPercent>75</xmp:RatingPercent>"
        val xmp50 = "<RatingPercent>50</RatingPercent>"
        val xmp25 = "<RatingPercent>25</RatingPercent>"
        val xmp10 = "<RatingPercent>10</RatingPercent>"

        assertEquals(5, XmpRatingParser.parseRating(xmp99))
        assertEquals(4, XmpRatingParser.parseRating(xmp75))
        assertEquals(3, XmpRatingParser.parseRating(xmp50))
        assertEquals(2, XmpRatingParser.parseRating(xmp25))
        assertEquals(1, XmpRatingParser.parseRating(xmp10))
    }

    @Test
    fun parseRating_withUnratedZero_returnsZero() {
        val xmp = "<xmp:Rating>0</xmp:Rating>"
        assertEquals(0, XmpRatingParser.parseRating(xmp))
    }

    @Test
    fun parseRating_withNoRating_returnsNull() {
        val xmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Sin calificar</dc:title>
              </rdf:Description>
            </x:xmpmeta>
        """.trimIndent()

        assertNull(XmpRatingParser.parseRating(xmp))
    }

    @Test
    fun parseCaptureDate_withValidIsoDate_returnsTimestamp() {
        val xmp = """
            <xmp:CreateDate>2024-05-18T14:30:00</xmp:CreateDate>
        """.trimIndent()

        val timestamp = XmpRatingParser.parseCaptureDate(xmp)
        assertNotNull(timestamp)
    }

    @Test
    fun extractXmpFromStream_findsEmbeddedPacket() {
        val fakeHeader = "SOME_JPEG_APP1_PREFIX_http://ns.adobe.com/xap/1.0/\u0000" +
                "<x:xmpmeta><xmp:Rating>5</xmp:Rating></x:xmpmeta>" +
                "<?xpacket end='w'?>TRAILING_BYTES"
        val stream = ByteArrayInputStream(fakeHeader.toByteArray(StandardCharsets.ISO_8859_1))

        val extracted = XmpRatingParser.extractXmpFromStream(stream)
        assertNotNull(extracted)
        val rating = XmpRatingParser.parseRating(extracted!!)
        assertEquals(5, rating)
    }
}
