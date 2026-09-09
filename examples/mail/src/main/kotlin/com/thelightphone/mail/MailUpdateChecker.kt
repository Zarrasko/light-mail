package com.thelightphone.mail

import com.thelightphone.mail.engine.MinimalJson
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MailUpdateCheckResult(
    val currentVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
)

class MailUpdateCheckException(message: String) : Exception(message)

/**
 * Checks GitHub Releases for a newer version of Mail than [currentVersion]. Read-only: this
 * never downloads or installs anything - the SDK's sandbox doesn't let a Tool launch the system
 * installer (or even open a browser), so the most this can do is tell you whether one exists.
 */
object MailUpdateChecker {
    private const val RELEASES_URL = "https://api.github.com/repos/Zarrasko/light-mail/releases/latest"
    private const val TIMEOUT_MS = 10000

    suspend fun checkForUpdate(currentVersion: String): MailUpdateCheckResult = withContext(Dispatchers.IO) {
        val json = fetch() ?: throw MailUpdateCheckException("Couldn't check for updates")
        val tagName = MinimalJson.getString(json, "tag_name")
            ?: throw MailUpdateCheckException("Couldn't read the latest version")
        val latestVersion = tagName.removePrefix("v")

        MailUpdateCheckResult(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            isUpdateAvailable = compareVersions(latestVersion, currentVersion) > 0,
        )
    }

    private fun fetch(): String? {
        val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /** Compares two "1.2.3"-style version strings numerically, component by component. */
    internal fun compareVersions(a: String, b: String): Int {
        val aParts = a.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val length = maxOf(aParts.size, bParts.size)
        for (i in 0 until length) {
            val diff = aParts.getOrElse(i) { 0 } - bParts.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }
}
