package com.thelightphone.mail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailFolderTest {

    private fun testAccount(
        sentFolder: MailFolderState = MailFolderState(),
        draftsFolder: MailFolderState = MailFolderState(),
        trashFolder: MailFolderState = MailFolderState(),
    ) = MailAccount(
        email = "person@example.com",
        imapHost = "imap.example.com",
        smtpHost = "smtp.example.com",
        sentFolder = sentFolder,
        draftsFolder = draftsFolder,
        trashFolder = trashFolder,
    )

    @Test
    fun `extraFolders is empty when nothing is shown`() {
        assertTrue(testAccount().extraFolders().isEmpty())
    }

    @Test
    fun `extraFolders excludes a shown folder that hasn't resolved a name yet`() {
        val account = testAccount(sentFolder = MailFolderState(shown = true, resolvedName = null))
        assertTrue(account.extraFolders().isEmpty())
    }

    @Test
    fun `extraFolders excludes a resolved folder that isn't shown`() {
        val account = testAccount(sentFolder = MailFolderState(shown = false, resolvedName = "Sent Items"))
        assertTrue(account.extraFolders().isEmpty())
    }

    @Test
    fun `extraFolders includes shown, resolved folders in Sent, Drafts, Trash order`() {
        val account = testAccount(
            sentFolder = MailFolderState(shown = true, resolvedName = "Sent Items"),
            draftsFolder = MailFolderState(shown = true, resolvedName = "Drafts"),
            trashFolder = MailFolderState(shown = true, resolvedName = "Deleted Items"),
        )

        assertEquals(
            listOf(
                MailFolder("Sent", "Sent Items"),
                MailFolder("Drafts", "Drafts"),
                MailFolder("Trash", "Deleted Items"),
            ),
            account.extraFolders(),
        )
    }

    @Test
    fun `folderState returns the matching field for each special folder type`() {
        val account = testAccount(
            sentFolder = MailFolderState(shown = true, resolvedName = "Sent"),
            draftsFolder = MailFolderState(shown = false, resolvedName = "Drafts"),
            trashFolder = MailFolderState(shown = true, resolvedName = "Trash"),
        )

        assertEquals(account.sentFolder, account.folderState(MailSpecialFolderType.SENT))
        assertEquals(account.draftsFolder, account.folderState(MailSpecialFolderType.DRAFTS))
        assertEquals(account.trashFolder, account.folderState(MailSpecialFolderType.TRASH))
    }
}
