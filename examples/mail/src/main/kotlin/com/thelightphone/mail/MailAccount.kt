package com.thelightphone.mail

/** A single configured mail account. The app supports any number of these. */
data class MailAccount(
    val id: Long = 0,
    val email: String,
    val password: String = "",
    val imapHost: String,
    val imapPort: Int = 993,
    val smtpHost: String,
    val smtpPort: Int = 465,
    val smtpUseStartTls: Boolean = false,
    val lastSeenUid: Long = 0,
    val messageLimitOverride: Int? = null,
    val notificationsEnabled: Boolean = true,
    val authType: MailAuthType = MailAuthType.PASSWORD,
    /** Only set when [authType] is [MailAuthType.MICROSOFT_OAUTH]; access tokens aren't persisted. */
    val microsoftRefreshToken: String = "",
)
