package com.thelightphone.mail.engine

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImapLineReaderTest {

    private fun readerFor(raw: String) = ImapLineReader(ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8)))

    @Test
    fun `reads a simple line without a literal`() {
        val reader = readerFor("A0001 OK LOGIN completed\r\n")
        assertEquals("A0001 OK LOGIN completed", reader.readLine())
    }

    @Test
    fun `returns null at end of stream`() {
        val reader = readerFor("")
        assertNull(reader.readLine())
    }

    @Test
    fun `splices a literal's bytes into the logical line`() {
        // The literal body deliberately contains a bare CRLF, which a naive line
        // reader would mistake for the end of the response.
        val raw = "* 1 FETCH (BODY[TEXT] {13}\r\nline1\r\nline2)\r\n"
        val reader = readerFor(raw)
        val line = reader.readLine()
        assertEquals("* 1 FETCH (BODY[TEXT] {13}\r\nline1\r\nline2)", line)
    }

    @Test
    fun `reads multiple lines in sequence after a literal`() {
        val raw = "* 1 FETCH (BODY[TEXT] {5}\r\nhello)\r\nA0001 OK FETCH completed\r\n"
        val reader = readerFor(raw)
        assertEquals("* 1 FETCH (BODY[TEXT] {5}\r\nhello)", reader.readLine())
        assertEquals("A0001 OK FETCH completed", reader.readLine())
    }
}
