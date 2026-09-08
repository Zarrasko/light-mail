package com.thelightphone.mail.engine

import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

/**
 * Looks up IMAP/SMTP settings for a domain from Mozilla's public autoconfig database
 * (the same ISPDB Thunderbird itself queries), for providers not in [KnownProviders].
 * No API key or auth needed - it's a plain public HTTPS+XML lookup.
 */
object MozillaIspdbClient {

    private const val TIMEOUT_MS = 8000

    suspend fun lookup(domain: String): ServerConfig? = withContext(Dispatchers.IO) {
        val xml = fetch(domain) ?: return@withContext null
        parse(xml)
    }

    private fun fetch(domain: String): String? {
        return try {
            val url = URL("https://autoconfig.thunderbird.net/v1.1/${domain.lowercase()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Package-visible for testing against canned ISPDB responses without a network call. */
    internal fun parse(xml: String): ServerConfig? {
        val document = try {
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xml.byteInputStream())
        } catch (e: Exception) {
            return null
        }

        val incoming = document.getElementsByTagName("incomingServer")
            .toElementList()
            .firstOrNull { it.getAttribute("type").equals("imap", ignoreCase = true) }
            ?: return null
        val outgoing = document.getElementsByTagName("outgoingServer")
            .toElementList()
            .firstOrNull { it.getAttribute("type").equals("smtp", ignoreCase = true) }
            ?: return null

        val imapHost = incoming.childText("hostname") ?: return null
        val imapPort = incoming.childText("port")?.toIntOrNull() ?: return null
        val smtpHost = outgoing.childText("hostname") ?: return null
        val smtpPort = outgoing.childText("port")?.toIntOrNull() ?: return null
        val smtpSocketType = outgoing.childText("socketType")

        return ServerConfig(
            imapHost = imapHost,
            imapPort = imapPort,
            smtpHost = smtpHost,
            smtpPort = smtpPort,
            smtpUseStartTls = smtpSocketType.equals("STARTTLS", ignoreCase = true),
        )
    }

    private fun org.w3c.dom.NodeList.toElementList(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.childText(tagName: String): String? =
        getElementsByTagName(tagName).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
