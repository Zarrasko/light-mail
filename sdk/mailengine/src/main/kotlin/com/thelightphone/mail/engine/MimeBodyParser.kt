package com.thelightphone.mail.engine

/**
 * Extracts readable text from a fetched message body, given its Content-Type and
 * Content-Transfer-Encoding headers. Handles the common real-world case of
 * multipart/alternative (and multipart/mixed wrapping it) by picking the text/plain
 * part; there is no attachment or text/html rendering support.
 */
object MimeBodyParser {

    private val boundaryRegex = Regex("""boundary\s*=\s*"?([^;"\r\n]+)"?""", RegexOption.IGNORE_CASE)

    fun isMultipart(contentType: String?): Boolean =
        contentType?.trim()?.startsWith("multipart/", ignoreCase = true) == true

    fun extractBoundary(contentType: String?): String? =
        contentType?.let { boundaryRegex.find(it)?.groupValues?.get(1)?.trim() }

    /** Decodes a fetched message body per its headers into readable text. */
    fun extractReadableText(rawBody: String, headers: Map<String, String>): String {
        val contentType = headers["content-type"]

        if (isMultipart(contentType)) {
            val boundary = extractBoundary(contentType)
                ?: return "(Couldn't find this multipart message's boundary marker)"
            return findPlainTextPart(rawBody, boundary)
                ?: "(This message doesn't include a plain-text version)"
        }

        return decodePart(rawBody, contentType, headers["content-transfer-encoding"])
    }

    /**
     * Splits a multipart body on its boundary and returns the decoded text/plain part,
     * recursing into any nested multipart part (e.g. multipart/mixed wrapping
     * multipart/alternative). Returns null if no text/plain part is found anywhere.
     *
     * Each part looks like `<CRLF><headers, possibly none><CRLF><CRLF><body>` right after its
     * "--boundary" delimiter - the header/body separator is a blank line, which is a bare
     * "\r\n\r\n" run when headers are non-empty but sits right at the start (matching at index 0)
     * when a part has no headers at all. Searching for that separator on the untouched segment
     * (rather than trimming leading CRLFs first) is what makes the empty-headers case work.
     */
    private fun findPlainTextPart(body: String, boundary: String): String? {
        val delimiter = "--$boundary"
        val segments = body.split(delimiter)

        for (segment in segments) {
            if (segment.isBlank() || segment.trimStart('\r', '\n').startsWith("--")) {
                continue // preamble/epilogue or the closing "--boundary--" delimiter
            }

            val crlfIndex = segment.indexOf("\r\n\r\n")
            val lfIndex = segment.indexOf("\n\n")
            val (headerEnd, separatorLength) = when {
                crlfIndex != -1 && (lfIndex == -1 || crlfIndex <= lfIndex) -> crlfIndex to 4
                lfIndex != -1 -> lfIndex to 2
                else -> continue // no header/body separator found - malformed part, skip it
            }

            val partHeaders = RawHeaders.parse(segment.substring(0, headerEnd))
            val partBody = segment.substring(headerEnd + separatorLength).trimEnd('\r', '\n')
            val partContentType = partHeaders["content-type"]

            if (isMultipart(partContentType)) {
                val nestedBoundary = extractBoundary(partContentType) ?: continue
                findPlainTextPart(partBody, nestedBoundary)?.let { return it }
                continue
            }

            val isPlainText = partContentType == null || partContentType.trim().startsWith("text/plain", ignoreCase = true)
            if (isPlainText) {
                return decodePart(partBody, partContentType, partHeaders["content-transfer-encoding"])
            }
        }
        return null
    }

    private fun decodePart(body: String, contentType: String?, transferEncoding: String?): String {
        val charset = MimeUtils.charsetFromContentType(contentType)
        return when (transferEncoding?.trim()?.lowercase()) {
            "quoted-printable" -> MimeUtils.decodeQuotedPrintable(body, charset)
            "base64" -> MimeUtils.decodeBase64Body(body, charset)
            else -> body
        }
    }
}
