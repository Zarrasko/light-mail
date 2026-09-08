package com.thelightphone.mail.engine

/**
 * Pure parsing of untagged IMAP FETCH response lines, split out from [ImapClient] so it can
 * be unit tested against canned response text instead of only against a live server.
 */
object ImapFetchParser {

    private val fetchLineRegex = Regex("""^\* \d+ FETCH""")
    private val uidRegex = Regex("""UID (\d+)""")
    private val flagsRegex = Regex("""FLAGS \(([^)]*)\)""")
    private val literalLengthRegex = Regex("""\{(\d+)\}""")

    /** Parses one `* n FETCH (UID ... FLAGS (...) BODY[HEADER.FIELDS (...)] {n}\n<headers>)` line. */
    fun parseSummary(line: String): ImapMessageSummary? {
        if (!fetchLineRegex.containsMatchIn(line)) return null
        val uid = uidRegex.find(line)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val flags = flagsRegex.find(line)?.groupValues?.get(1).orEmpty()
        val headerText = extractLiteral(line, afterMarker = "BODY[HEADER.FIELDS").orEmpty()
        val headers = RawHeaders.parse(headerText)
        return ImapMessageSummary(
            uid = uid,
            from = headers["from"].orEmpty(),
            subject = headers["subject"].orEmpty(),
            date = headers["date"].orEmpty(),
            isSeen = flags.contains("\\Seen"),
        )
    }

    /** Parses one `* n FETCH (... BODY[TEXT] {n}\n<raw body>)` line, returning the raw (still-encoded) body. */
    fun parseBodyText(line: String): String? = extractLiteral(line, afterMarker = "BODY[TEXT]")

    /** Parses a `BODY[HEADER.FIELDS (...)] {n}\n<headers>` literal from a FETCH response line. */
    fun parseHeaderFields(line: String): String? = extractLiteral(line, afterMarker = "BODY[HEADER.FIELDS")

    /**
     * Extracts a literal's content by its declared `{n}` length rather than matching to the end
     * of the line, so trailing syntax (e.g. the FETCH response's closing paren) isn't swept in.
     *
     * Note: `n` is a byte count on the wire, but [ImapLineReader] has already decoded the literal
     * to a Kotlin String by then, so this counts UTF-16 chars post-decode instead. That's exact
     * for ASCII literal content (the common case: RFC 2047 header text and base64/quoted-printable
     * bodies are both 7-bit ASCII on the wire) but would misalign for a raw 8-bit literal.
     */
    private fun extractLiteral(line: String, afterMarker: String): String? {
        val markerIndex = line.indexOf(afterMarker)
        if (markerIndex == -1) return null
        val lengthMatch = literalLengthRegex.find(line, markerIndex) ?: return null
        val length = lengthMatch.groupValues[1].toIntOrNull() ?: return null

        var contentStart = lengthMatch.range.last + 1
        if (line.getOrNull(contentStart) == '\r') contentStart++
        if (line.getOrNull(contentStart) == '\n') contentStart++

        val contentEnd = contentStart + length
        if (contentEnd > line.length) return null
        return line.substring(contentStart, contentEnd)
    }
}
