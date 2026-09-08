package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class MimeBodyParserTest {

    @Test
    fun `single-part 7bit body passes through unchanged`() {
        val headers = mapOf("content-type" to "text/plain; charset=utf-8")
        assertEquals("Hello there", MimeBodyParser.extractReadableText("Hello there", headers))
    }

    @Test
    fun `single-part quoted-printable body is decoded`() {
        val headers = mapOf(
            "content-type" to "text/plain; charset=utf-8",
            "content-transfer-encoding" to "quoted-printable",
        )
        assertEquals("Café", MimeBodyParser.extractReadableText("Caf=C3=A9", headers))
    }

    @Test
    fun `multipart alternative picks the text plain part over html`() {
        val boundary = "----boundary123"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: text/plain; charset=utf-8\r\n")
            append("\r\n")
            append("Plain version\r\n")
            append("--$boundary\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("\r\n")
            append("<p>HTML version</p>\r\n")
            append("--$boundary--\r\n")
        }
        val headers = mapOf("content-type" to "multipart/alternative; boundary=\"$boundary\"")

        assertEquals("Plain version", MimeBodyParser.extractReadableText(body, headers))
    }

    @Test
    fun `multipart alternative decodes the plain part's own encoding`() {
        val boundary = "b1"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: text/plain; charset=utf-8\r\n")
            append("Content-Transfer-Encoding: quoted-printable\r\n")
            append("\r\n")
            append("Caf=C3=A9\r\n")
            append("--$boundary--\r\n")
        }
        val headers = mapOf("content-type" to "multipart/alternative; boundary=$boundary")

        assertEquals("Café", MimeBodyParser.extractReadableText(body, headers))
    }

    @Test
    fun `nested multipart mixed wrapping alternative finds the plain part`() {
        val innerBoundary = "inner"
        val outerBoundary = "outer"
        val innerPart = buildString {
            append("--$innerBoundary\r\n")
            append("Content-Type: text/plain; charset=utf-8\r\n")
            append("\r\n")
            append("Nested plain text\r\n")
            append("--$innerBoundary--\r\n")
        }
        val body = buildString {
            append("--$outerBoundary\r\n")
            append("Content-Type: multipart/alternative; boundary=$innerBoundary\r\n")
            append("\r\n")
            append(innerPart)
            append("--$outerBoundary--\r\n")
        }
        val headers = mapOf("content-type" to "multipart/mixed; boundary=$outerBoundary")

        assertEquals("Nested plain text", MimeBodyParser.extractReadableText(body, headers))
    }

    @Test
    fun `multipart with no plain text part returns a friendly fallback`() {
        val boundary = "b1"
        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("\r\n")
            append("<p>Only HTML here</p>\r\n")
            append("--$boundary--\r\n")
        }
        val headers = mapOf("content-type" to "multipart/alternative; boundary=$boundary")

        assertEquals(
            "(This message doesn't include a plain-text version)",
            MimeBodyParser.extractReadableText(body, headers),
        )
    }

    @Test
    fun `part with no content-type header defaults to plain text`() {
        val boundary = "b1"
        val body = buildString {
            append("--$boundary\r\n")
            append("\r\n")
            append("No content-type here\r\n")
            append("--$boundary--\r\n")
        }
        val headers = mapOf("content-type" to "multipart/mixed; boundary=$boundary")

        assertEquals("No content-type here", MimeBodyParser.extractReadableText(body, headers))
    }
}
