package com.thelightphone.mail.engine

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImapMessageSummary(
    val uid: Long,
    val from: String,
    val subject: String,
    val date: String,
    val isSeen: Boolean,
)

class ImapCommandException(message: String) : Exception(message)

/**
 * A minimal, narrowly-scoped IMAP4rev1 (RFC 3501) client: connect, log in, list an
 * inbox's most recent messages, and fetch one message's plain-text body.
 *
 * No IDLE/push, no folder management beyond INBOX, no multipart/attachment handling.
 * XOAUTH2 is not implemented, so provider accounts that require OAuth for IMAP
 * (most Gmail/Outlook accounts) need an app-specific password instead.
 */
class ImapClient(
    private val host: String,
    private val port: Int,
    private val useTls: Boolean = true,
) {
    private lateinit var socket: Socket
    private lateinit var reader: ImapLineReader
    private lateinit var writer: OutputStream
    private val tagCounter = AtomicInteger(1)

    suspend fun connect() = withContext(Dispatchers.IO) {
        socket = if (useTls) {
            SSLSocketFactory.getDefault().createSocket(host, port)
        } else {
            Socket(host, port)
        }
        reader = ImapLineReader(socket.getInputStream())
        writer = BufferedOutputStream(socket.getOutputStream())
        readGreeting()
    }

    private fun readGreeting() {
        val line = reader.readLine() ?: throw ImapCommandException("Connection closed before greeting")
        if (!line.startsWith("* OK")) throw ImapCommandException("Unexpected greeting: $line")
    }

    suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "LOGIN ${quote(username)} ${quote(password)}")
        readUntilTagged(tag)
    }

    /** Selects INBOX and returns the number of messages in it. */
    suspend fun selectInbox(): Int = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "SELECT INBOX")
        var messageCount = 0
        val existsRegex = Regex("""^\* (\d+) EXISTS""")
        readUntilTagged(tag) { line ->
            existsRegex.find(line)?.let { messageCount = it.groupValues[1].toInt() }
        }
        messageCount
    }

    /**
     * Fetches summaries for the given 1-based sequence range (e.g. "12:20"), newest last,
     * as IMAP returns them.
     */
    suspend fun fetchSummaries(sequenceRange: String): List<ImapMessageSummary> = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "FETCH $sequenceRange (UID FLAGS BODY.PEEK[HEADER.FIELDS (FROM SUBJECT DATE)])")

        val summaries = mutableListOf<ImapMessageSummary>()
        readUntilTagged(tag) { line ->
            ImapFetchParser.parseSummary(line)?.let { summaries.add(it) }
        }
        summaries
    }

    /**
     * Fetches the readable text of the message with the given UID: for a multipart message
     * (e.g. multipart/alternative), extracts and decodes the text/plain part; otherwise decodes
     * the single body part per its own Content-Transfer-Encoding. No attachment or HTML rendering.
     */
    suspend fun fetchPlainTextBody(uid: Long): String = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(
            tag,
            "UID FETCH $uid (BODY.PEEK[HEADER.FIELDS (CONTENT-TYPE CONTENT-TRANSFER-ENCODING)] BODY.PEEK[TEXT])",
        )

        var rawBody = ""
        var headers: Map<String, String> = emptyMap()
        readUntilTagged(tag) { line ->
            ImapFetchParser.parseBodyText(line)?.let { rawBody = it }
            ImapFetchParser.parseHeaderFields(line)?.let { headers = RawHeaders.parse(it) }
        }
        MimeBodyParser.extractReadableText(rawBody, headers)
    }

    /**
     * Permanently deletes the message with the given UID: flags it `\Deleted`, then expunges.
     * Uses plain `EXPUNGE` rather than `UID EXPUNGE` (a UIDPLUS extension not guaranteed to be
     * present), so this also removes any other message a caller happened to flag `\Deleted` -
     * harmless here since nothing else in this client ever sets that flag.
     */
    suspend fun deleteMessage(uid: Long) = withContext(Dispatchers.IO) {
        val storeTag = nextTag()
        sendCommand(storeTag, "UID STORE $uid +FLAGS (\\Deleted)")
        readUntilTagged(storeTag)

        val expungeTag = nextTag()
        sendCommand(expungeTag, "EXPUNGE")
        readUntilTagged(expungeTag)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            val tag = nextTag()
            sendCommand(tag, "LOGOUT")
            readUntilTagged(tag)
        }
        runCatching { socket.close() }
    }

    private fun sendCommand(tag: String, command: String) {
        writer.write("$tag $command\r\n".toByteArray(Charsets.UTF_8))
        writer.flush()
    }

    private fun readUntilTagged(tag: String, onUntagged: (String) -> Unit = {}) {
        while (true) {
            val line = reader.readLine() ?: throw ImapCommandException("Connection closed mid-response")
            if (line.startsWith("$tag ")) {
                if (!line.startsWith("$tag OK", ignoreCase = true)) {
                    throw ImapCommandException("Command failed: $line")
                }
                return
            }
            onUntagged(line)
        }
    }

    private fun nextTag() = "A%04d".format(tagCounter.getAndIncrement())

    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
