package com.thelightphone.mail.engine

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawMessageBuilderTest {

    @Test
    fun `includes required headers`() {
        val message = RawMessageBuilder.buildPlainText(
            from = "me@example.com",
            to = "you@example.com",
            subject = "Hi",
            body = "Hello there",
        )

        assertTrue(message.contains("From: me@example.com\r\n"))
        assertTrue(message.contains("To: you@example.com\r\n"))
        assertTrue(message.contains("Subject: Hi\r\n"))
        assertTrue(message.contains("Content-Transfer-Encoding: base64\r\n"))
    }

    @Test
    fun `body is base64 encoded and round-trips`() {
        val message = RawMessageBuilder.buildPlainText(
            from = "me@example.com",
            to = "you@example.com",
            subject = "Hi",
            body = "Hello there",
        )

        val encodedBody = message.substringAfter("\r\n\r\n").replace("\r\n", "")
        val decoded = String(Base64.getDecoder().decode(encodedBody), Charsets.UTF_8)
        assertEquals("Hello there", decoded)
    }

    @Test
    fun `non-ASCII subject is encoded as an RFC 2047 word`() {
        val message = RawMessageBuilder.buildPlainText(
            from = "me@example.com",
            to = "you@example.com",
            subject = "Café",
            body = "Hello",
        )

        assertTrue(message.contains("Subject: =?UTF-8?B?"))
    }
}
