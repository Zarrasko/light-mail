package com.thelightphone.mail.engine

import java.util.Base64

/**
 * Minimal MIME helpers for a plain-text-only mail client: decoding RFC 2047 encoded
 * header words (so non-ASCII subjects/names display correctly) and the two
 * Content-Transfer-Encodings a text/plain body is realistically found in.
 *
 * No multipart/attachment handling - out of scope for the constrained LP3 reader.
 */
object MimeUtils {

    private val encodedWordRegex = Regex(
        pattern = """=\?([^?]+)\?([bBqQ])\?([^?]*)\?=""",
    )

    /** Decodes RFC 2047 encoded-words (e.g. "=?UTF-8?B?...?=") that appear in header values. */
    fun decodeHeaderWords(value: String): String {
        if (!value.contains("=?")) return value

        val builder = StringBuilder()
        var lastEnd = 0
        for (match in encodedWordRegex.findAll(value)) {
            val between = value.substring(lastEnd, match.range.first)
            // Collapse whitespace that only separates two adjacent encoded-words, per RFC 2047.
            if (builder.isNotEmpty() && between.isBlank() && lastEnd != 0) {
                // drop the separator
            } else {
                builder.append(between)
            }

            val charsetName = match.groupValues[1]
            val encoding = match.groupValues[2]
            val encodedText = match.groupValues[3]
            builder.append(decodeEncodedWord(charsetName, encoding, encodedText))
            lastEnd = match.range.last + 1
        }
        builder.append(value.substring(lastEnd))
        return builder.toString()
    }

    private fun decodeEncodedWord(charsetName: String, encoding: String, text: String): String {
        val charset = runCatching { charset(charsetName) }.getOrDefault(Charsets.UTF_8)
        return try {
            when (encoding.lowercase()) {
                "b" -> String(Base64.getDecoder().decode(text), charset)
                "q" -> decodeQEncoding(text, charset)
                else -> text
            }
        } catch (e: IllegalArgumentException) {
            text
        }
    }

    private fun decodeQEncoding(text: String, charset: java.nio.charset.Charset): String {
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '_' -> {
                    bytes.write(' '.code)
                    i++
                }
                c == '=' && i + 2 < text.length -> {
                    val hex = text.substring(i + 1, i + 3)
                    bytes.write(hex.toInt(16))
                    i += 3
                }
                else -> {
                    bytes.write(c.code)
                    i++
                }
            }
        }
        return String(bytes.toByteArray(), charset)
    }

    /** Decodes a quoted-printable body per RFC 2045. */
    fun decodeQuotedPrintable(text: String, charset: java.nio.charset.Charset = Charsets.UTF_8): String {
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '=' && i + 2 < text.length && text[i + 1] == '\r' && text[i + 2] == '\n' -> {
                    // Soft line break - the '=' at end of line joins it with the next.
                    i += 3
                }
                c == '=' && i + 1 < text.length && text[i + 1] == '\n' -> {
                    i += 2
                }
                c == '=' && i + 2 < text.length -> {
                    val hex = text.substring(i + 1, i + 3)
                    val value = hex.toIntOrNull(16)
                    if (value != null) {
                        bytes.write(value)
                        i += 3
                    } else {
                        bytes.write(c.code)
                        i++
                    }
                }
                else -> {
                    bytes.write(c.code)
                    i++
                }
            }
        }
        return String(bytes.toByteArray(), charset)
    }

    /** Decodes a base64 body, tolerating the line-wrapped form found in MIME messages. */
    fun decodeBase64Body(text: String, charset: java.nio.charset.Charset = Charsets.UTF_8): String {
        val cleaned = text.filterNot { it == '\r' || it == '\n' || it.isWhitespace() }
        return try {
            String(Base64.getDecoder().decode(cleaned), charset)
        } catch (e: IllegalArgumentException) {
            text
        }
    }

    /** Extracts a header's charset from a Content-Type value, e.g. `text/plain; charset=iso-8859-1`. */
    fun charsetFromContentType(contentType: String?): java.nio.charset.Charset {
        val match = contentType?.let { Regex("""charset="?([^;"\s]+)"?""", RegexOption.IGNORE_CASE).find(it) }
        val name = match?.groupValues?.get(1) ?: return Charsets.UTF_8
        return runCatching { charset(name) }.getOrDefault(Charsets.UTF_8)
    }

    private fun charset(name: String) = java.nio.charset.Charset.forName(name)
}
