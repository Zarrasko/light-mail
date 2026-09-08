package com.thelightphone.mail.engine

/**
 * Extracts the bare address from an RFC 5322 mailbox for use in an SMTP envelope command
 * (`MAIL FROM:<...>` / `RCPT TO:<...>`), which must not contain a display name.
 *
 * A header value like `From:`/`To:` may be either a bare address ("alice@example.com") or a
 * display name plus address ("Alice <alice@example.com>") - the two forms callers realistically
 * pass in, e.g. when prefilling a reply's "To" field from a message's decoded From header.
 */
object SmtpAddress {
    private val angleAddressRegex = Regex("""<([^<>]+)>""")

    fun bareAddress(value: String): String {
        val match = angleAddressRegex.find(value)
        return (match?.groupValues?.get(1) ?: value).trim()
    }
}
