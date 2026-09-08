package com.thelightphone.mail.engine

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerAutodiscoveryTest {

    @Test
    fun `known provider resolves without a network call`() = runTest {
        val config = ServerAutodiscovery.lookup("someone@fastmail.com")
        assertEquals("imap.fastmail.com", config?.imapHost)
    }

    @Test
    fun `domain matching ignores case`() = runTest {
        val config = ServerAutodiscovery.lookup("someone@FastMail.COM")
        assertEquals("imap.fastmail.com", config?.imapHost)
    }

    @Test
    fun `an address with no domain resolves to nothing`() = runTest {
        assertNull(ServerAutodiscovery.lookup("not-an-email"))
    }
}
