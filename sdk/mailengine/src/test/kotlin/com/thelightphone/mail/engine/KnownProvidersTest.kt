package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KnownProvidersTest {

    @Test
    fun `resolves Fastmail`() {
        val config = KnownProviders.forDomain("fastmail.com")
        assertEquals("imap.fastmail.com", config?.imapHost)
        assertEquals(993, config?.imapPort)
        assertEquals("smtp.fastmail.com", config?.smtpHost)
        assertEquals(587, config?.smtpPort)
        assertEquals(true, config?.smtpUseStartTls)
    }

    @Test
    fun `resolves iCloud domain aliases`() {
        assertEquals("imap.mail.me.com", KnownProviders.forDomain("icloud.com")?.imapHost)
        assertEquals("imap.mail.me.com", KnownProviders.forDomain("me.com")?.imapHost)
        assertEquals("imap.mail.me.com", KnownProviders.forDomain("mac.com")?.imapHost)
    }

    @Test
    fun `domain matching is case-insensitive`() {
        assertEquals("imap.fastmail.com", KnownProviders.forDomain("FastMail.COM")?.imapHost)
    }

    @Test
    fun `unknown domain returns null`() {
        assertNull(KnownProviders.forDomain("some-random-domain.example"))
    }
}
