package com.thelightphone.mail.engine

/**
 * Resolves IMAP/SMTP settings for an email address so people only need to enter an email +
 * password: a hardcoded table of common providers first (instant, offline), then a network
 * lookup against Mozilla's public autoconfig database. Returns null if neither finds anything,
 * in which case the caller should fall back to manual entry.
 */
object ServerAutodiscovery {

    suspend fun lookup(email: String): ServerConfig? {
        val domain = email.substringAfterLast('@', missingDelimiterValue = "").trim()
        if (domain.isEmpty()) return null

        KnownProviders.forDomain(domain)?.let { return it }
        return MozillaIspdbClient.lookup(domain)
    }
}
