package com.thelightphone.mail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MailInboxFetchPlannerTest {

    @Test
    fun `effectiveLimit uses the account override when present`() {
        assertEquals(50, MailInboxFetchPlanner.effectiveLimit(accountOverride = 50, globalDefault = MailMessageLimit.TEN))
    }

    @Test
    fun `effectiveLimit falls back to the global default when there is no override`() {
        assertEquals(10, MailInboxFetchPlanner.effectiveLimit(accountOverride = null, globalDefault = MailMessageLimit.TEN))
    }

    @Test
    fun `effectiveLimit treats an explicit zero override as unlimited, not falling back`() {
        assertEquals(0, MailInboxFetchPlanner.effectiveLimit(accountOverride = 0, globalDefault = MailMessageLimit.TEN))
    }

    @Test
    fun `sequenceRangeFor windows to the most recent messages`() {
        assertEquals("81:100", MailInboxFetchPlanner.sequenceRangeFor(messageCount = 100, limit = 20))
    }

    @Test
    fun `sequenceRangeFor clamps the start to 1 when the limit exceeds the mailbox size`() {
        assertEquals("1:5", MailInboxFetchPlanner.sequenceRangeFor(messageCount = 5, limit = 20))
    }

    @Test
    fun `sequenceRangeFor fetches everything when the limit is unlimited`() {
        assertEquals("1:100", MailInboxFetchPlanner.sequenceRangeFor(messageCount = 100, limit = 0))
    }

    @Test
    fun `sequenceRangeFor returns null for an empty mailbox`() {
        assertNull(MailInboxFetchPlanner.sequenceRangeFor(messageCount = 0, limit = 20))
    }
}
