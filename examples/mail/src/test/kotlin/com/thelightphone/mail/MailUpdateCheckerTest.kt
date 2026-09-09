package com.thelightphone.mail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailUpdateCheckerTest {

    @Test
    fun `equal versions compare as zero`() {
        assertEquals(0, MailUpdateChecker.compareVersions("0.2.0", "0.2.0"))
    }

    @Test
    fun `a higher patch version compares greater`() {
        assertTrue(MailUpdateChecker.compareVersions("0.2.1", "0.2.0") > 0)
    }

    @Test
    fun `a higher minor version compares greater even with a lower patch`() {
        assertTrue(MailUpdateChecker.compareVersions("0.3.0", "0.2.9") > 0)
    }

    @Test
    fun `numeric comparison is used, not lexical - 0-10-0 beats 0-9-0`() {
        assertTrue(MailUpdateChecker.compareVersions("0.10.0", "0.9.0") > 0)
    }

    @Test
    fun `a lower version compares less`() {
        assertTrue(MailUpdateChecker.compareVersions("0.1.0", "0.2.0") < 0)
    }

    @Test
    fun `missing trailing components are treated as zero`() {
        assertEquals(0, MailUpdateChecker.compareVersions("0.2", "0.2.0"))
        assertTrue(MailUpdateChecker.compareVersions("0.2.1", "0.2") > 0)
    }
}
