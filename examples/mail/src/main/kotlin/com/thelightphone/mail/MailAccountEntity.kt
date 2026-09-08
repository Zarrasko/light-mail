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
)
