package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RawHeadersTest {

    @Test
    fun `parses simple headers`() {
        val headers = RawHeaders.parse("From: alice@example.com\r\nSubject: Hello\r\n")
        assertEquals("alice@example.com", headers["from"])
        assertEquals("Hello", headers["subject"])
    }

    @Test
    fun `unfolds a continuation line`() {
        val headers = RawHeaders.parse("Subject: Hello\r\n World\r\n")
        assertEquals("Hello World", headers["subject"])
    }

    @Test
    fun `header names are matched case-insensitively`() {
        val headers = RawHeaders.parse("FROM: alice@example.com\r\n")
        assertEquals("alice@example.com", headers["from"])
    }

    @Test
    fun `missing header returns null`() {
        val headers = RawHeaders.parse("From: alice@example.com\r\n")
        assertNull(headers["subject"])
    }

    @Test
    fun `decodes encoded words within header values`() {
        val headers = RawHeaders.parse("Subject: =?UTF-8?B?Q2Fmw6k=?=\r\n")
        assertEquals("Café", headers["subject"])
    }
}
