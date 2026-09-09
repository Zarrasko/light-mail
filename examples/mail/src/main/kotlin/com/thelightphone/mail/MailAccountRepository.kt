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
                authType = account.authType.name,
                encryptedMicrosoftRefreshToken = account.microsoftRefreshToken
                    .takeIf { it.isNotEmpty() }
                    ?.let(cipher::encrypt),
            ),
        )
    }

    suspend fun deleteAccount(id: Long) {
        dao.delete(id)
    }

    suspend fun updateLastSeenUid(id: Long, uid: Long) {
        dao.updateLastSeenUid(id, uid)
    }

    suspend fun updateMessageLimitOverride(id: Long, limit: Int?) {
        dao.updateMessageLimitOverride(id, limit)
    }

    suspend fun updateNotificationsEnabled(id: Long, enabled: Boolean) {
        dao.updateNotificationsEnabled(id, enabled)
    }

    /** Microsoft rotates the refresh token on every use; call this after every token refresh. */
    suspend fun updateMicrosoftRefreshToken(id: Long, refreshToken: String) {
        dao.updateMicrosoftRefreshToken(id, cipher.encrypt(refreshToken))
    }

    suspend fun updateSentFolder(id: Long, state: MailFolderState) {
        dao.updateSentFolder(id, state.shown, state.resolvedName)
    }

    suspend fun updateDraftsFolder(id: Long, state: MailFolderState) {
        dao.updateDraftsFolder(id, state.shown, state.resolvedName)
    }

    suspend fun updateTrashFolder(id: Long, state: MailFolderState) {
        dao.updateTrashFolder(id, state.shown, state.resolvedName)
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
        messageLimitOverride = messageLimitOverride,
        notificationsEnabled = notificationsEnabled,
        authType = MailAuthType.valueOf(authType),
        microsoftRefreshToken = encryptedMicrosoftRefreshToken?.let(cipher::decrypt).orEmpty(),
        sentFolder = sentFolder.toState(),
        draftsFolder = draftsFolder.toState(),
        trashFolder = trashFolder.toState(),
    )

    private fun MailFolderColumns.toState() = MailFolderState(shown = shown, resolvedName = resolvedName)

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
