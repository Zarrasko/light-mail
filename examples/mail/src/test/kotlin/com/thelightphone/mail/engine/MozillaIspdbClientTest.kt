package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MozillaIspdbClientTest {

    @Test
    fun `parses a typical autoconfig response`() {
        val xml = """
            <clientConfig version="1.1">
              <emailProvider id="example.com">
                <domain>example.com</domain>
                <incomingServer type="pop3">
                  <hostname>pop.example.com</hostname>
                  <port>995</port>
                  <socketType>SSL</socketType>
                </incomingServer>
                <incomingServer type="imap">
                  <hostname>imap.example.com</hostname>
                  <port>993</port>
                  <socketType>SSL</socketType>
                </incomingServer>
                <outgoingServer type="smtp">
                  <hostname>smtp.example.com</hostname>
                  <port>587</port>
                  <socketType>STARTTLS</socketType>
                </outgoingServer>
              </emailProvider>
            </clientConfig>
        """.trimIndent()

        val config = MozillaIspdbClient.parse(xml)

        assertEquals("imap.example.com", config?.imapHost)
        assertEquals(993, config?.imapPort)
        assertEquals("smtp.example.com", config?.smtpHost)
        assertEquals(587, config?.smtpPort)
        assertTrue(config!!.smtpUseStartTls)
    }

    @Test
    fun `SSL socket type means implicit TLS, not STARTTLS`() {
        val xml = """
            <clientConfig version="1.1">
              <emailProvider id="example.com">
                <incomingServer type="imap">
                  <hostname>imap.example.com</hostname>
                  <port>993</port>
                  <socketType>SSL</socketType>
                </incomingServer>
                <outgoingServer type="smtp">
                  <hostname>smtp.example.com</hostname>
                  <port>465</port>
                  <socketType>SSL</socketType>
                </outgoingServer>
              </emailProvider>
            </clientConfig>
        """.trimIndent()

        val config = MozillaIspdbClient.parse(xml)

        assertEquals(false, config?.smtpUseStartTls)
    }

    @Test
    fun `returns null when there is no imap incomingServer`() {
        val xml = """
            <clientConfig version="1.1">
              <emailProvider id="example.com">
                <incomingServer type="pop3">
                  <hostname>pop.example.com</hostname>
                  <port>995</port>
                  <socketType>SSL</socketType>
                </incomingServer>
                <outgoingServer type="smtp">
                  <hostname>smtp.example.com</hostname>
                  <port>587</port>
                  <socketType>STARTTLS</socketType>
                </outgoingServer>
              </emailProvider>
            </clientConfig>
        """.trimIndent()

        assertNull(MozillaIspdbClient.parse(xml))
    }

    @Test
    fun `returns null for malformed XML`() {
        assertNull(MozillaIspdbClient.parse("not xml at all"))
    }
}
