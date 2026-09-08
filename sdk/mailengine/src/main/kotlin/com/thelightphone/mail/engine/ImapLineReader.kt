package com.thelightphone.mail.engine

import java.io.InputStream

/**
 * Reads IMAP response lines (RFC 3501), resolving `{n}` literals inline.
 *
 * A literal is a byte count in curly braces at the end of a line, e.g.
 * `* 12 FETCH (BODY[TEXT] {45}\r\n<45 raw bytes>)\r\n`. Those 45 bytes may contain
 * anything, including bare CR/LF, so a plain `BufferedReader.readLine()` would
 * corrupt them. This reader treats a literal's bytes as text (this client only
 * handles text/plain bodies) and splices them into the logical line before
 * continuing to read up to the terminating CRLF.
 */
class ImapLineReader(private val input: InputStream) {

    private val literalRegex = Regex("""\{(\d+)\}\r?$""")

    /** Reads one full logical IMAP response line, with any literals inlined as text. */
    fun readLine(): String? {
        val builder = StringBuilder()
        while (true) {
            val rawLine = readRawLine() ?: return if (builder.isEmpty()) null else builder.toString()
            builder.append(rawLine)

            val literalMatch = literalRegex.find(rawLine)
            if (literalMatch == null) {
                return builder.toString()
            }

            val byteCount = literalMatch.groupValues[1].toInt()
            val literalBytes = readExactly(byteCount)
            // readRawLine() strips the CRLF that terminates the "{n}" marker line; put it back
            // so callers see the literal exactly as "{n}\r\n<content>", matching the wire format.
            // CR/LF inside the literal itself must not be treated as line terminators, so it's
            // decoded here rather than re-entering readRawLine().
            builder.append("\r\n")
            builder.append(String(literalBytes, Charsets.UTF_8))
        }
    }

    private fun readRawLine(): String? {
        val bytes = java.io.ByteArrayOutputStream()
        var prevWasCr = false
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (bytes.size() == 0) null else bytes.toString("UTF-8")
            }
            if (b == '\n'.code && prevWasCr) {
                val content = bytes.toByteArray()
                return String(content, 0, content.size - 1, Charsets.UTF_8)
            }
            bytes.write(b)
            prevWasCr = b == '\r'.code
        }
    }

    private fun readExactly(count: Int): ByteArray {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read == -1) throw java.io.EOFException("Server closed connection mid-literal")
            offset += read
        }
        return buffer
    }
}
