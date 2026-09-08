package com.thelightphone.mail.engine

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MicrosoftDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int,
)

data class MicrosoftTokens(
    val accessToken: String,
    val refreshToken: String,
    val idToken: String?,
)

sealed class MicrosoftTokenPollResult {
    data class Success(val tokens: MicrosoftTokens) : MicrosoftTokenPollResult()
    object Pending : MicrosoftTokenPollResult()
    object SlowDown : MicrosoftTokenPollResult()
    data class Failed(val reason: String) : MicrosoftTokenPollResult()
}

class MicrosoftOAuthException(message: String) : Exception(message)

/**
 * OAuth 2.0 Device Authorization Grant (RFC 8628) against the Microsoft identity platform, for
 * signing in to Outlook/Microsoft 365 accounts without a WebView or system browser - neither of
 * which the SDK's sandbox permits. The user visits a short URL on any device of their choosing
 * and enters a short code there; this client just polls until they finish.
 */
class MicrosoftOAuthClient(private val clientId: String) {

    suspend fun requestDeviceCode(scopes: String): MicrosoftDeviceCode = withContext(Dispatchers.IO) {
        val body = formEncode("client_id" to clientId, "scope" to scopes)
        val (code, json) = postWithStatus(DEVICE_CODE_URL, body)
        if (code != HttpURLConnection.HTTP_OK || json == null) {
            throw MicrosoftOAuthException("Couldn't reach Microsoft sign-in")
        }

        MicrosoftDeviceCode(
            deviceCode = MinimalJson.getString(json, "device_code")
                ?: throw MicrosoftOAuthException("Malformed response from Microsoft sign-in"),
            userCode = MinimalJson.getString(json, "user_code")
                ?: throw MicrosoftOAuthException("Malformed response from Microsoft sign-in"),
            verificationUri = MinimalJson.getString(json, "verification_uri")
                ?: throw MicrosoftOAuthException("Malformed response from Microsoft sign-in"),
            expiresInSeconds = MinimalJson.getInt(json, "expires_in") ?: 900,
            intervalSeconds = MinimalJson.getInt(json, "interval") ?: 5,
        )
    }

    suspend fun pollForToken(deviceCode: String): MicrosoftTokenPollResult = withContext(Dispatchers.IO) {
        val body = formEncode(
            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
            "client_id" to clientId,
            "device_code" to deviceCode,
        )
        parseTokenResponse(postWithStatus(TOKEN_URL, body))
    }

    /**
     * Exchanges a refresh token for a fresh access token. Microsoft rotates the refresh token
     * on every use, so callers must persist the new one returned here or the next refresh will
     * fail.
     */
    suspend fun refreshTokens(refreshToken: String, scopes: String): MicrosoftTokens = withContext(Dispatchers.IO) {
        val body = formEncode(
            "grant_type" to "refresh_token",
            "client_id" to clientId,
            "refresh_token" to refreshToken,
            "scope" to scopes,
        )
        when (val result = parseTokenResponse(postWithStatus(TOKEN_URL, body))) {
            is MicrosoftTokenPollResult.Success -> result.tokens
            is MicrosoftTokenPollResult.Failed -> throw MicrosoftOAuthException(result.reason)
            else -> throw MicrosoftOAuthException("Couldn't refresh Microsoft sign-in - please sign in again")
        }
    }

    private fun parseTokenResponse(response: Pair<Int, String?>): MicrosoftTokenPollResult {
        val (code, json) = response
        if (json == null) return MicrosoftTokenPollResult.Failed("Couldn't reach Microsoft sign-in")

        if (code == HttpURLConnection.HTTP_OK) {
            val accessToken = MinimalJson.getString(json, "access_token")
            val refreshToken = MinimalJson.getString(json, "refresh_token")
            return if (accessToken != null && refreshToken != null) {
                val idToken = MinimalJson.getString(json, "id_token")
                MicrosoftTokenPollResult.Success(MicrosoftTokens(accessToken, refreshToken, idToken))
            } else {
                MicrosoftTokenPollResult.Failed("Malformed response from Microsoft sign-in")
            }
        }

        return when (MinimalJson.getString(json, "error")) {
            "authorization_pending" -> MicrosoftTokenPollResult.Pending
            "slow_down" -> MicrosoftTokenPollResult.SlowDown
            "expired_token" -> MicrosoftTokenPollResult.Failed("That code expired - try again")
            "authorization_declined" -> MicrosoftTokenPollResult.Failed("Sign-in was declined")
            else -> MicrosoftTokenPollResult.Failed(
                MinimalJson.getString(json, "error_description") ?: "Microsoft sign-in failed",
            )
        }
    }

    private fun postWithStatus(url: String, body: String): Pair<Int, String?> {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            code to stream?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            -1 to null
        } finally {
            connection.disconnect()
        }
    }

    private fun formEncode(vararg params: Pair<String, String>): String =
        params.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

    companion object {
        private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/devicecode"
        private const val TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        private const val TIMEOUT_MS = 15000

        /** IMAP/SMTP plus enough identity scope to read the signed-in address off the ID token. */
        const val OUTLOOK_SCOPES = "openid profile email offline_access " +
            "https://outlook.office.com/IMAP.AccessAsUser.All https://outlook.office.com/SMTP.Send"

        /** Reads the "preferred_username" (or "email") claim out of a JWT ID token's payload. */
        fun emailFromIdToken(idToken: String?): String? {
            val payload = idToken?.split(".")?.getOrNull(1) ?: return null
            val json = try {
                String(Base64.getUrlDecoder().decode(payload.padToBase64()), Charsets.UTF_8)
            } catch (e: Exception) {
                return null
            }
            return MinimalJson.getString(json, "preferred_username") ?: MinimalJson.getString(json, "email")
        }

        private fun String.padToBase64(): String {
            val remainder = length % 4
            return if (remainder == 0) this else this + "=".repeat(4 - remainder)
        }
    }
}

/** Pulls flat string/number fields out of a JSON object without a parsing dependency. */
internal object MinimalJson {
    fun getString(json: String, key: String): String? {
        val regex = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(json)?.groupValues?.get(1)?.let(::unescape)
    }

    fun getInt(json: String, key: String): Int? {
        val regex = Regex(""""${Regex.escape(key)}"\s*:\s*(-?\d+)""")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun unescape(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\/", "/")
}
