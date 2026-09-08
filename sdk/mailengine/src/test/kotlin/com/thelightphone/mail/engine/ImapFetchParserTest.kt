package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImapFetchParserTest {

    @Test
    fun `parseSummary extracts uid, flags, and decoded headers`() {
        val headerBlock = "From: Alice <alice@example.com>\r\nSubject: Hello\r\nDate: Mon, 1 Jan 2026 09:00:00 +0000\r\n\r\n"
        val line = "* 3 FETCH (UID 42 FLAGS (\\Seen) BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {${headerBlock.length}}\n$headerBlock)"

        val summary = ImapFetchParser.parseSummary(line)

        assertEquals(42L, summary?.uid)
        assertEquals("Alice <alice@example.com>", summary?.from)
        assertEquals("Hello", summary?.subject)
        assertTrue(summary!!.isSeen)
    }

    @Test
    fun `parseSummary marks unseen when Seen flag is absent`() {
        val headerBlock = "From: Bob <bob@example.com>\r\nSubject: Hi\r\nDate: Mon, 1 Jan 2026 09:00:00 +0000\r\n\r\n"
        val line = "* 4 FETCH (UID 43 FLAGS (\\Recent) BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {${headerBlock.length}}\n$headerBlock)"

        val summary = ImapFetchParser.parseSummary(line)

        assertFalse(summary!!.isSeen)
    }

    @Test
    fun `parseSummary decodes an encoded-word subject`() {
        val headerBlock = "From: Alice <alice@example.com>\r\nSubject: =?UTF-8?B?Q2Fmw6k=?=\r\nDate: x\r\n\r\n"
        val line = "* 5 FETCH (UID 44 FLAGS () BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {${headerBlock.length}}\n$headerBlock)"

        assertEquals("Café", ImapFetchParser.parseSummary(line)?.subject)
    }

    @Test
    fun `parseSummary returns null for a non-FETCH line`() {
        assertNull(ImapFetchParser.parseSummary("* 5 EXISTS"))
    }

    @Test
    fun `parseBodyText extracts the raw literal body`() {
        val body = "Hi there,\r\nSee you soon."
        val line = "* 3 FETCH (UID 42 BODY[TEXT] {${body.length}}\n$body)"

        assertEquals(body, ImapFetchParser.parseBodyText(line))
    }

    @Test
    fun `parseBodyText returns null when the line has no body literal`() {
        assertNull(ImapFetchParser.parseBodyText("* 3 FETCH (UID 42 FLAGS (\\Seen))"))
    }

    @Test
    fun `parseHeaderFields extracts a Content-Type header literal alongside a body literal`() {
        val headerBlock = "Content-Type: multipart/alternative; boundary=abc\r\n\r\n"
        val body = "Hi there"
        val line = "* 3 FETCH (UID 42 BODY[HEADER.FIELDS (CONTENT-TYPE)] {${headerBlock.length}}\n$headerBlock " +
            "BODY[TEXT] {${body.length}}\n$body)"

        assertEquals(headerBlock, ImapFetchParser.parseHeaderFields(line))
        assertEquals(body, ImapFetchParser.parseBodyText(line))
    }
}
