package com.thelightphone.mail.engine

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.Base64
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
 * A minimal, narrowly-scoped IMAP4rev1 (RFC 3501) client: connect, log in, list a folder's most
 * recent messages, and fetch one message's plain-text body.
 *
 * No IDLE/push, no multipart/attachment handling. Folder support is limited to SELECTing a
 * folder by name - there's no LIST command, so [resolveFolder] guesses a provider's name for a
 * special-use folder (Sent, Drafts, Trash) from a candidate list instead of asking the server.
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

    /** Logs in via SASL XOAUTH2 (RFC 7628) with a bearer access token instead of a password. */
    suspend fun loginXOAuth2(username: String, accessToken: String) = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "AUTHENTICATE XOAUTH2 ${xoauth2SaslResponse(username, accessToken)}")

        val first = reader.readLine() ?: throw ImapCommandException("Connection closed mid-response")
        when {
            first.startsWith("+") -> {
                // The server rejected the token and is asking for a continuation (RFC 7628
                // 3.2.3); send an empty response so it emits the final tagged failure instead
                // of hanging.
                writer.write("\r\n".toByteArray(Charsets.UTF_8))
                writer.flush()
                readUntilTagged(tag)
            }
            first.startsWith("$tag ") -> {
                if (!first.startsWith("$tag OK", ignoreCase = true)) {
                    throw ImapCommandException("Command failed: $first")
                }
            }
            else -> readUntilTagged(tag)
        }
    }

    /** Selects the given folder (e.g. "INBOX") and returns the number of messages in it. */
    suspend fun selectFolder(folderName: String): Int = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "SELECT ${quote(folderName)}")
        var messageCount = 0
        val existsRegex = Regex("""^\* (\d+) EXISTS""")
        readUntilTagged(tag) { line ->
            existsRegex.find(line)?.let { messageCount = it.groupValues[1].toInt() }
        }
        messageCount
    }

    /**
     * Tries each candidate name in order and returns the first that SELECTs successfully, or
     * null if none exist. There's no LIST command here, so this is how a provider's name for a
     * special-use folder (Sent, Drafts, Trash) gets guessed instead of discovered - it won't
     * find an uncommonly-named folder.
     */
    suspend fun resolveFolder(candidates: List<String>): String? = withContext(Dispatchers.IO) {
        for (candidate in candidates) {
            val tag = nextTag()
            sendCommand(tag, "SELECT ${quote(candidate)}")
            val found = try {
                readUntilTagged(tag)
                true
            } catch (e: ImapCommandException) {
                false
            }
            if (found) return@withContext candidate
        }
        null
    }

    /**
     * Appends a raw RFC 5322 message (see [RawMessageBuilder]) to [folderName], e.g. saving a
     * draft. [flags] is an IMAP flag list like `\Draft`; pass an empty string for none.
     */
    suspend fun appendMessage(folderName: String, rawMessage: String, flags: String = "") =
        withContext(Dispatchers.IO) {
            val bytes = rawMessage.toByteArray(Charsets.UTF_8)
            val tag = nextTag()
            val flagPart = if (flags.isBlank()) "" else " ($flags)"
            sendCommand(tag, "APPEND ${quote(folderName)}$flagPart {${bytes.size}}")

            val continuation = reader.readLine() ?: throw ImapCommandException("Connection closed mid-response")
            if (!continuation.startsWith("+")) {
                throw ImapCommandException("Server didn't accept APPEND literal: $continuation")
            }

            writer.write(bytes)
            writer.write("\r\n".toByteArray(Charsets.UTF_8))
            writer.flush()
            readUntilTagged(tag)
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
     * Marks the message with the given UID `\Seen`. [fetchPlainTextBody] deliberately uses
     * `BODY.PEEK` so reading a body never has this side effect on its own - callers that want
     * standard "opening a message marks it read" behavior call this separately.
     */
    suspend fun markSeen(uid: Long) = withContext(Dispatchers.IO) {
        val tag = nextTag()
        sendCommand(tag, "UID STORE $uid +FLAGS (\\Seen)")
        readUntilTagged(tag)
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

    private fun xoauth2SaslResponse(username: String, accessToken: String): String {
        // RFC 7628 3.1: fields are separated by CTL-A (\u0001), with a trailing pair of them.
        val raw = "user=$username\u0001auth=Bearer $accessToken\u0001\u0001"
        return Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
    }
}
