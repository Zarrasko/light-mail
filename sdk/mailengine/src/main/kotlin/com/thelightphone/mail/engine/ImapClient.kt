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

/** One entry from an IMAP `LIST` response: its name and any flags (e.g. `\Sent`) it carries. */
data class ImapListedFolder(val name: String, val flags: Set<String>)

/** Matches `* LIST (\flag \flag) "delim" name` - name may or may not be quoted. */
private val LIST_LINE_REGEX = Regex("""^\* LIST \(([^)]*)\) "([^"]*)" (.+)$""")

/**
 * A minimal, narrowly-scoped IMAP4rev1 (RFC 3501) client: connect, log in, list a folder's most
 * recent messages, and fetch one message's plain-text body.
 *
 * No IDLE/push, no multipart/attachment handling. Folder support is limited to SELECTing a
 * folder by name, plus enough of LIST to discover a special-use folder's real name (see
 * [resolveSpecialUseFolder]) rather than guess it.
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
     * null if none exist. This is a last-resort guess for when the server doesn't support
     * SPECIAL-USE (see [resolveSpecialUseFolder]) - if a provider happens to have more than one
     * folder matching the candidate list (e.g. a real "Sent" folder plus an empty "Sent Items"
     * left over from a migration), this can lock onto the wrong one, since it has no way to
     * tell them apart beyond which name happens to exist first.
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
     * Resolves the folder for a special use (e.g. "\Sent", "\Drafts", "\Trash") using the
     * server's own SPECIAL-USE tagging (RFC 6154) when it supports the extension - this is what
     * a real mail client uses to find these folders, so it's authoritative where [resolveFolder]
     * is just a guess. Falls back to [candidates] only when the server doesn't support
     * SPECIAL-USE at all.
     */
    suspend fun resolveSpecialUseFolder(useFlag: String, candidates: List<String>): String? =
        withContext(Dispatchers.IO) {
            val folders = listFoldersWithSpecialUse()
            val flagged = folders?.firstOrNull { useFlag in it.flags }
            flagged?.name ?: resolveFolder(candidates)
        }

    /**
     * Lists every folder with its SPECIAL-USE flags via `LIST (SPECIAL-USE) "" "*"`, or null if
     * the server rejects the SPECIAL-USE extension outright (as opposed to just returning zero
     * flagged folders, which is a normal, valid response for a server that supports the
     * extension but doesn't tag anything).
     */
    private fun listFoldersWithSpecialUse(): List<ImapListedFolder>? {
        val tag = nextTag()
        sendCommand(tag, """LIST (SPECIAL-USE) "" "*"""")

        val folders = mutableListOf<ImapListedFolder>()
        return try {
            readUntilTagged(tag) { line -> parseListLine(line)?.let(folders::add) }
            folders
        } catch (e: ImapCommandException) {
            null
        }
    }

    /** Package-visible for testing against canned LIST responses without a network call. */
    internal fun parseListLine(line: String): ImapListedFolder? {
        val match = LIST_LINE_REGEX.find(line) ?: return null
        val flags = match.groupValues[1].split(" ").filter { it.isNotBlank() }.toSet()
        val rawName = match.groupValues[3].trim()
        val name = if (rawName.startsWith("\"") && rawName.endsWith("\"")) {
            rawName.removeSurrounding("\"").replace("\\\"", "\"").replace("\\\\", "\\")
        } else {
            rawName
        }
        return ImapListedFolder(name, flags)
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
     * Permanently deletes the message with the given UID from the currently selected folder:
     * flags it `\Deleted`, then expunges. Uses plain `EXPUNGE` rather than `UID EXPUNGE` (a
     * UIDPLUS extension not guaranteed to be present), so this also removes any other message a
     * caller happened to flag `\Deleted` - harmless here since nothing else in this client ever
     * sets that flag outside of this same expunge step.
     */
    suspend fun deleteMessage(uid: Long) = withContext(Dispatchers.IO) {
        expungeFromCurrentFolder(uid)
    }

    /**
     * Moves the message with the given UID into [destinationFolder]: copies it there, then
     * removes it from the currently selected folder the same way [deleteMessage] does. This is
     * the standard manual "move" recipe for servers that don't support the MOVE extension
     * (RFC 6851) - COPY, flag `\Deleted`, EXPUNGE - rather than relying on that extension.
     */
    suspend fun moveMessage(uid: Long, destinationFolder: String) = withContext(Dispatchers.IO) {
        val copyTag = nextTag()
        sendCommand(copyTag, "UID COPY $uid ${quote(destinationFolder)}")
        readUntilTagged(copyTag)

        expungeFromCurrentFolder(uid)
    }

    private fun expungeFromCurrentFolder(uid: Long) {
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
