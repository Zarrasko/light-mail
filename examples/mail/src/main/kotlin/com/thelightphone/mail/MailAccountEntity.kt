package com.thelightphone.mail

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mail_account")
data class MailAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: ByteArray,
    @ColumnInfo(name = "imap_host") val imapHost: String,
    @ColumnInfo(name = "imap_port") val imapPort: Int,
    @ColumnInfo(name = "smtp_host") val smtpHost: String,
    @ColumnInfo(name = "smtp_port") val smtpPort: Int,
    @ColumnInfo(name = "smtp_use_start_tls") val smtpUseStartTls: Boolean,
    /** Highest inbox UID seen by the last background sync; 0 means never synced yet. */
    @ColumnInfo(name = "last_seen_uid", defaultValue = "0") val lastSeenUid: Long = 0,
    /** Overrides [MailSettingsRepository.defaultMessageLimit]'s count for this account; null inherits it. */
    @ColumnInfo(name = "message_limit_override") val messageLimitOverride: Int? = null,
    /** ANDed with [MailSettingsRepository.notificationsEnabled] - a global "off" always wins. */
    @ColumnInfo(name = "notifications_enabled", defaultValue = "1") val notificationsEnabled: Boolean = true,
    @ColumnInfo(name = "auth_type", defaultValue = "PASSWORD") val authType: String = "PASSWORD",
    /** Encrypted like [encryptedPassword]; only set for [MailAuthType.MICROSOFT_OAUTH] accounts. */
    @ColumnInfo(name = "encrypted_microsoft_refresh_token") val encryptedMicrosoftRefreshToken: ByteArray? = null,
)
