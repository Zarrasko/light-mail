package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class SmtpAddressTest {

    @Test
    fun `bare address is returned unchanged`() {
        assertEquals("alice@example.com", SmtpAddress.bareAddress("alice@example.com"))
    }

    @Test
    fun `display name and address extracts the address`() {
        assertEquals("alice@example.com", SmtpAddress.bareAddress("Alice <alice@example.com>"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("alice@example.com", SmtpAddress.bareAddress("  alice@example.com  "))
    }
}
