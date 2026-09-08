package com.thelightphone.mail.engine

/**
 * Server settings for providers known to work with plain IMAP/SMTP LOGIN + an app-specific
 * password (no OAuth). Checked before falling back to [MozillaIspdbClient]'s network lookup,
 * so the most common providers resolve instantly and offline.
 */
object KnownProviders {

    private val byDomain: Map<String, ServerConfig> = buildMap {
        val fastmail = ServerConfig(
            imapHost = "imap.fastmail.com",
            imapPort = 993,
            smtpHost = "smtp.fastmail.com",
            smtpPort = 587,
            smtpUseStartTls = true,
        )
        for (domain in listOf("fastmail.com", "fastmail.fm", "fastmail.net")) put(domain, fastmail)

        val icloud = ServerConfig(
            imapHost = "imap.mail.me.com",
            imapPort = 993,
            smtpHost = "smtp.mail.me.com",
            smtpPort = 587,
            smtpUseStartTls = true,
        )
        for (domain in listOf("icloud.com", "me.com", "mac.com")) put(domain, icloud)

        val yahoo = ServerConfig(
            imapHost = "imap.mail.yahoo.com",
            imapPort = 993,
            smtpHost = "smtp.mail.yahoo.com",
            smtpPort = 465,
            smtpUseStartTls = false,
        )
        for (domain in listOf("yahoo.com", "ymail.com", "rocketmail.com")) put(domain, yahoo)

        put(
            "zoho.com",
            ServerConfig(
                imapHost = "imap.zoho.com",
                imapPort = 993,
                smtpHost = "smtp.zoho.com",
                smtpPort = 587,
                smtpUseStartTls = true,
            ),
        )

        put(
            "gmx.com",
            ServerConfig(
                imapHost = "imap.gmx.com",
                imapPort = 993,
                smtpHost = "mail.gmx.com",
                smtpPort = 587,
                smtpUseStartTls = true,
            ),
        )
    }

    fun forDomain(domain: String): ServerConfig? = byDomain[domain.lowercase()]
}
