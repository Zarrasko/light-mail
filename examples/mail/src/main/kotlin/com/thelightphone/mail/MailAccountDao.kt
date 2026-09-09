package com.thelightphone.mail

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MailAccountDao {
    @Insert
    suspend fun insert(account: MailAccountEntity): Long

    @Query("SELECT * FROM mail_account ORDER BY email COLLATE NOCASE")
    suspend fun list(): List<MailAccountEntity>

    @Query("SELECT * FROM mail_account WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): MailAccountEntity?

    @Query("DELETE FROM mail_account WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE mail_account SET last_seen_uid = :uid WHERE id = :id")
    suspend fun updateLastSeenUid(id: Long, uid: Long)

    @Query("UPDATE mail_account SET message_limit_override = :limit WHERE id = :id")
    suspend fun updateMessageLimitOverride(id: Long, limit: Int?)

    @Query("UPDATE mail_account SET notifications_enabled = :enabled WHERE id = :id")
    suspend fun updateNotificationsEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE mail_account SET encrypted_microsoft_refresh_token = :token WHERE id = :id")
    suspend fun updateMicrosoftRefreshToken(id: Long, token: ByteArray)

    @Query("UPDATE mail_account SET sent_shown = :shown, sent_resolved_name = :resolvedName WHERE id = :id")
    suspend fun updateSentFolder(id: Long, shown: Boolean, resolvedName: String?)

    @Query("UPDATE mail_account SET drafts_shown = :shown, drafts_resolved_name = :resolvedName WHERE id = :id")
    suspend fun updateDraftsFolder(id: Long, shown: Boolean, resolvedName: String?)

    @Query("UPDATE mail_account SET trash_shown = :shown, trash_resolved_name = :resolvedName WHERE id = :id")
    suspend fun updateTrashFolder(id: Long, shown: Boolean, resolvedName: String?)
}
