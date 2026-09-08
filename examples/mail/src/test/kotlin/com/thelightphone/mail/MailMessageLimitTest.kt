package com.thelightphone.mail

import kotlin.test.Test
import kotlin.test.assertEquals

class MailMessageLimitTest {

    @Test
    fun `fromCount maps known counts to their enum entries`() {
        assertEquals(MailMessageLimit.TEN, MailMessageLimit.fromCount(10))
        assertEquals(MailMessageLimit.TWENTY, MailMessageLimit.fromCount(20))
        assertEquals(MailMessageLimit.FIFTY, MailMessageLimit.fromCount(50))
        assertEquals(MailMessageLimit.UNLIMITED, MailMessageLimit.fromCount(0))
    }

    @Test
    fun `fromCount falls back to the default for an unrecognized count`() {
        assertEquals(MailMessageLimit.DEFAULT, MailMessageLimit.fromCount(37))
    }
}
