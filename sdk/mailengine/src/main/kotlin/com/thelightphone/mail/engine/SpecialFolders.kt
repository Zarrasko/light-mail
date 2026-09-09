package com.thelightphone.mail.engine

/**
 * Common IMAP folder names for the special-use mailboxes providers don't agree on, in the order
 * [ImapClient.resolveFolder] should try them. Covers Fastmail, iCloud, Gmail, Outlook/Microsoft
 * 365, and Yahoo; an uncommonly-named folder won't be found (see [ImapClient.resolveFolder]).
 */
object SpecialFolders {
    val SENT_CANDIDATES = listOf("Sent", "Sent Items", "Sent Messages", "Sent Mail", "INBOX.Sent")
    val DRAFTS_CANDIDATES = listOf("Drafts", "Draft", "INBOX.Drafts")
    val TRASH_CANDIDATES = listOf("Trash", "Deleted Items", "Deleted Messages", "INBOX.Trash")
}
