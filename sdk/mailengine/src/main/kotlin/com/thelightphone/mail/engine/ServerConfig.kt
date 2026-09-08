package com.thelightphone.mail.engine

/** Discovered IMAP/SMTP connection settings for an email domain. */
data class ServerConfig(
    val imapHost: String,
    val imapPort: Int,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUseStartTls: Boolean,
)
