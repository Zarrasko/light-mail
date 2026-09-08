package com.thelightphone.mail.engine

/** Parses an RFC 5322 header block (unfolded continuation lines, colon-separated fields). */
object RawHeaders {

    fun parse(headerBlock: String): Map<String, String> {
        val unfolded = mutableListOf<String>()
        for (rawLine in headerBlock.split("\r\n", "\n")) {
            if (rawLine.isEmpty()) continue
            if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && unfolded.isNotEmpty()) {
                unfolded[unfolded.size - 1] = unfolded.last() + " " + rawLine.trim()
            } else {
                unfolded.add(rawLine)
            }
        }

        val result = LinkedHashMap<String, String>()
        for (line in unfolded) {
            val colonIndex = line.indexOf(':')
            if (colonIndex <= 0) continue
            val name = line.substring(0, colonIndex).trim().lowercase()
            val value = line.substring(colonIndex + 1).trim()
            result[name] = MimeUtils.decodeHeaderWords(value)
        }
        return result
    }
}
