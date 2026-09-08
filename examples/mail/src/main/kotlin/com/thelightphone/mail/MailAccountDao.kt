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
}
