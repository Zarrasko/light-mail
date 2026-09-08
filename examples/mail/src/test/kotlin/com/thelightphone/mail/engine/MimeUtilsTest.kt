package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class MimeUtilsTest {

    @Test
    fun `decodeHeaderWords leaves plain ASCII untouched`() {
        assertEquals("Hello there", MimeUtils.decodeHeaderWords("Hello there"))
    }

    @Test
    fun `decodeHeaderWords decodes a base64 encoded word`() {
        // "Café" in UTF-8, base64-encoded.
        assertEquals("Café", MimeUtils.decodeHeaderWords("=?UTF-8?B?Q2Fmw6k=?="))
    }

    @Test
    fun `decodeHeaderWords decodes a Q encoded word with underscores as spaces`() {
        assertEquals("Hello World", MimeUtils.decodeHeaderWords("=?UTF-8?Q?Hello_World?="))
    }

    @Test
    fun `decodeHeaderWords decodes a Q encoded word with hex escapes`() {
        assertEquals("Café", MimeUtils.decodeHeaderWords("=?UTF-8?Q?Caf=C3=A9?="))
    }

    @Test
    fun `decodeHeaderWords handles mixed plain and encoded segments`() {
        assertEquals("Re: Café meeting", MimeUtils.decodeHeaderWords("Re: =?UTF-8?B?Q2Fmw6k=?= meeting"))
    }

    @Test
    fun `decodeQuotedPrintable joins soft line breaks`() {
        val input = "This is a long line that=\r\ncontinues here."
        assertEquals("This is a long line thatcontinues here.", MimeUtils.decodeQuotedPrintable(input))
    }

    @Test
    fun `decodeQuotedPrintable decodes hex escapes`() {
        assertEquals("Café", MimeUtils.decodeQuotedPrintable("Caf=C3=A9"))
    }

    @Test
    fun `decodeBase64Body strips line wrapping before decoding`() {
        val encoded = "SGVs\r\nbG8g\r\nV29y\r\nbGQ="
        assertEquals("Hello World", MimeUtils.decodeBase64Body(encoded))
    }

    @Test
    fun `charsetFromContentType extracts charset`() {
        val charset = MimeUtils.charsetFromContentType("text/plain; charset=iso-8859-1")
        assertEquals("ISO-8859-1", charset.name())
    }

    @Test
    fun `charsetFromContentType defaults to UTF-8 when absent`() {
        assertEquals(Charsets.UTF_8, MimeUtils.charsetFromContentType("text/plain"))
        assertEquals(Charsets.UTF_8, MimeUtils.charsetFromContentType(null))
    }
}
