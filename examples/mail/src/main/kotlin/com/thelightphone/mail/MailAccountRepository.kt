package com.thelightphone.mail

class MailAccountRepository private constructor(
    database: MailDatabase,
    private val cipher: MailSecretCipher,
) {
    private val dao = database.accountDao()

    suspend fun listAccounts(): List<MailAccount> = dao.list().map { it.toAccount() }

    suspend fun getAccount(id: Long): MailAccount? = dao.get(id)?.toAccount()

    suspend fun addAccount(account: MailAccount): Long {
        return dao.insert(
            MailAccountEntity(
                email = account.email,
                encryptedPassword = cipher.encrypt(account.password),
                imapHost = account.imapHost,
                imapPort = account.imapPort,
                smtpHost = account.smtpHost,
                smtpPort = account.smtpPort,
                smtpUseStartTls = account.smtpUseStartTls,
            ),
        )
    }

    suspend fun deleteAccount(id: Long) {
        dao.delete(id)
    }

    suspend fun updateLastSeenUid(id: Long, uid: Long) {
        dao.updateLastSeenUid(id, uid)
    }

    private fun MailAccountEntity.toAccount() = MailAccount(
        id = id,
        email = email,
        password = cipher.decrypt(encryptedPassword),
        imapHost = imapHost,
        imapPort = imapPort,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        smtpUseStartTls = smtpUseStartTls,
        lastSeenUid = lastSeenUid,
    )

    companion object {
        const val DATABASE_NAME = "mail_account.db"

        @Volatile
        private var instance: MailAccountRepository? = null

        fun getInstance(databaseProvider: () -> MailDatabase): MailAccountRepository {
            return instance ?: synchronized(this) {
                instance ?: MailAccountRepository(
                    database = databaseProvider(),
                    cipher = MailSecretCipher(),
                ).also { instance = it }
            }
        }
    }
}
