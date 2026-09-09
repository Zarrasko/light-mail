package com.thelightphone.mail

import com.thelightphone.mail.engine.SpecialFolders

/** A navigable mailbox within an account: the always-present Inbox, or a resolved special-use folder. */
data class MailFolder(val label: String, val imapName: String) {
    companion object {
        val INBOX = MailFolder(label = "Inbox", imapName = "INBOX")
    }
}

/** Per-account visibility + resolved-name result for one special-use folder. */
data class MailFolderState(
    val shown: Boolean = false,
    /** Cached provider-specific IMAP name; null until resolution succeeds. */
    val resolvedName: String? = null,
)

enum class MailSpecialFolderType(val label: String, val specialUseFlag: String, val candidates: List<String>) {
    SENT("Sent", "\\Sent", SpecialFolders.SENT_CANDIDATES),
    DRAFTS("Drafts", "\\Drafts", SpecialFolders.DRAFTS_CANDIDATES),
    TRASH("Trash", "\\Trash", SpecialFolders.TRASH_CANDIDATES),
}

/** The folders this account should show, beyond the always-present Inbox. */
fun MailAccount.extraFolders(): List<MailFolder> = listOfNotNull(
    sentFolder.takeIf { it.shown }?.resolvedName?.let { MailFolder(MailSpecialFolderType.SENT.label, it) },
    draftsFolder.takeIf { it.shown }?.resolvedName?.let { MailFolder(MailSpecialFolderType.DRAFTS.label, it) },
    trashFolder.takeIf { it.shown }?.resolvedName?.let { MailFolder(MailSpecialFolderType.TRASH.label, it) },
)

fun MailAccount.folderState(type: MailSpecialFolderType): MailFolderState = when (type) {
    MailSpecialFolderType.SENT -> sentFolder
    MailSpecialFolderType.DRAFTS -> draftsFolder
    MailSpecialFolderType.TRASH -> trashFolder
}
