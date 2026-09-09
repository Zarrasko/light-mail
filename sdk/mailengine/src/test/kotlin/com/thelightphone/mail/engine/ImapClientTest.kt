package com.thelightphone.mail.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImapClientTest {

    private val client = ImapClient(host = "imap.example.com", port = 993)

    @Test
    fun `parses a quoted name with special-use flags`() {
        val folder = client.parseListLine("""* LIST (\HasNoChildren \Sent) "/" "Sent Messages"""")
        assertEquals(ImapListedFolder("Sent Messages", setOf("\\HasNoChildren", "\\Sent")), folder)
    }

    @Test
    fun `parses an unquoted name`() {
        val folder = client.parseListLine("""* LIST (\HasNoChildren) "/" INBOX""")
        assertEquals(ImapListedFolder("INBOX", setOf("\\HasNoChildren")), folder)
    }

    @Test
    fun `parses empty flags`() {
        val folder = client.parseListLine("""* LIST () "/" "Trash"""")
        assertEquals(ImapListedFolder("Trash", emptySet()), folder)
    }

    @Test
    fun `unescapes a quoted name containing an escaped quote`() {
        val folder = client.parseListLine("""* LIST (\Drafts) "/" "My \"Drafts\""""")
        assertEquals("My \"Drafts\"", folder?.name)
    }

    @Test
    fun `returns null for a non-LIST line`() {
        assertNull(client.parseListLine("""* 42 EXISTS"""))
        assertNull(client.parseListLine("""A0001 OK LIST completed"""))
    }
}
