package com.thelightphone.mail.engine

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID

/** Builds a minimal single-part, plain-text RFC 5322 message ready for SMTP DATA. */
object RawMessageBuilder {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

    fun buildPlainText(from: String, to: String, subject: String, body: String): String {
        val encodedBody = Base64.getEncoder().encodeToString(body.toByteArray(Charsets.UTF_8))
        val wrappedBody = encodedBody.chunked(76).joinToString("\r\n")

        return buildString {
            append("From: $from\r\n")
            append("To: $to\r\n")
            append("Subject: ${encodeHeaderIfNeeded(subject)}\r\n")
            append("Date: ${ZonedDateTime.now(ZoneOffset.UTC).format(dateFormatter)}\r\n")
            append("Message-ID: <${UUID.randomUUID()}@lightphone.local>\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=utf-8\r\n")
            append("Content-Transfer-Encoding: base64\r\n")
            append("\r\n")
            append(wrappedBody)
        }
    }

    private fun encodeHeaderIfNeeded(value: String): String {
        if (value.all { it.code < 128 }) return value
        val encoded = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        return "=?UTF-8?B?$encoded?="
    }
}
