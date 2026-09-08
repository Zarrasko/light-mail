package com.thelightphone.mail

import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.mail.engine.ImapMessageSummary
import com.thelightphone.sdk.LightJob
import com.thelightphone.sdk.LightJobHandler
import com.thelightphone.sdk.LightJobResult
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.postNotification

const val MAIL_SYNC_JOB_KEY = "mail-sync"
private const val NOTIFICATION_CHANNEL_ID = "new_mail"
private const val NOTIFICATION_CHANNEL_NAME = "New mail"
private const val CHECK_WINDOW_SIZE = 10

/**
 * Periodically checks every configured account's inbox for mail newer than the last check and
 * posts a local notification if there's any. This is poll-based, not push - there's no IMAP
 * IDLE/push support here, so "new mail" only becomes known at the next scheduled run (subject
 * to WorkManager's usual battery-driven delays, and its 15-minute floor on periodic work).
 */
@LightJob(MAIL_SYNC_JOB_KEY)
val mailSyncJob: LightJobHandler = { lightContext, _ ->
    val repository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(
            MailDatabase::class.java,
            MailAccountRepository.DATABASE_NAME,
            MAIL_DATABASE_MIGRATIONS,
        )
    }

    for (account in repository.listAccounts()) {
        checkAccountForNewMail(lightContext, repository, account)
    }

    LightJobResult.Success()
}

private suspend fun checkAccountForNewMail(
    lightContext: SealedLightContext,
    repository: MailAccountRepository,
    account: MailAccount,
) {
    val client = ImapClient(account.imapHost, account.imapPort)
    try {
        client.connect()
        client.login(account.email, account.password)
        val messageCount = client.selectInbox()
        if (messageCount == 0) return

        val start = maxOf(1, messageCount - CHECK_WINDOW_SIZE + 1)
        val summaries = client.fetchSummaries("$start:$messageCount")
        val newestUid = summaries.maxOfOrNull { it.uid } ?: return

        if (account.lastSeenUid == 0L) {
            // First sync for this account: establish a baseline instead of notifying about
            // mail that was already there before the account was added.
            repository.updateLastSeenUid(account.id, newestUid)
            return
        }

        val newMessages = summaries.filter { it.uid > account.lastSeenUid }
        if (newMessages.isNotEmpty()) {
            postNewMailNotification(lightContext, account, newMessages)
        }
        repository.updateLastSeenUid(account.id, newestUid)
    } catch (e: Exception) {
        // Skip this account this cycle - a transient network/auth issue shouldn't fail the
        // whole sync (or the other accounts' checks).
    } finally {
        client.logout()
    }
}

private fun postNewMailNotification(
    lightContext: SealedLightContext,
    account: MailAccount,
    newMessages: List<ImapMessageSummary>,
) {
    val title: String
    val text: String
    if (newMessages.size == 1) {
        val message = newMessages.first()
        title = message.from.ifBlank { account.email }
        text = message.subject.ifBlank { "(no subject)" }
    } else {
        title = account.email
        text = "${newMessages.size} new messages"
    }

    lightContext.postNotification(
        channelId = NOTIFICATION_CHANNEL_ID,
        channelName = NOTIFICATION_CHANNEL_NAME,
        notificationId = account.id.toInt(),
        title = title,
        text = text,
    )
}
