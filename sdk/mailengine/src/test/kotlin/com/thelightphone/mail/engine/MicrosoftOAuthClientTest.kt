package com.thelightphone.mail.engine

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinimalJsonTest {

    @Test
    fun `reads a string field among others`() {
        val json = """{"device_code":"abc123","user_code":"XYZ-789","expires_in":900}"""
        assertEquals("abc123", MinimalJson.getString(json, "device_code"))
        assertEquals("XYZ-789", MinimalJson.getString(json, "user_code"))
    }

    @Test
    fun `reads an int field`() {
        val json = """{"expires_in":900,"interval":5}"""
        assertEquals(900, MinimalJson.getInt(json, "expires_in"))
        assertEquals(5, MinimalJson.getInt(json, "interval"))
    }

    @Test
    fun `unescapes backslash-escaped characters in a string value`() {
        val json = """{"error_description":"Line one\nLine \"two\" with a \\ backslash"}"""
        assertEquals("Line one\nLine \"two\" with a \\ backslash", MinimalJson.getString(json, "error_description"))
    }

    @Test
    fun `returns null for a missing field`() {
        assertNull(MinimalJson.getString("""{"foo":"bar"}""", "missing"))
        assertNull(MinimalJson.getInt("""{"foo":"bar"}""", "missing"))
    }
}

class MicrosoftOAuthClientTest {

    @Test
    fun `reads preferred_username claim out of an ID token`() {
        val idToken = fakeIdToken("""{"preferred_username":"person@example.com","aud":"client-id"}""")
        assertEquals("person@example.com", MicrosoftOAuthClient.emailFromIdToken(idToken))
    }

    @Test
    fun `falls back to the email claim when preferred_username is absent`() {
        val idToken = fakeIdToken("""{"email":"person@example.com"}""")
        assertEquals("person@example.com", MicrosoftOAuthClient.emailFromIdToken(idToken))
    }

    @Test
    fun `returns null for a null or malformed token`() {
        assertNull(MicrosoftOAuthClient.emailFromIdToken(null))
        assertNull(MicrosoftOAuthClient.emailFromIdToken("not-a-jwt"))
    }

    /** Builds a JWT with the given payload JSON, base64url-encoded without padding like a real one. */
    private fun fakeIdToken(payloadJson: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"RS256"}""".toByteArray(Charsets.UTF_8))
        val payload = encoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        return "$header.$payload.signature"
    }
}
