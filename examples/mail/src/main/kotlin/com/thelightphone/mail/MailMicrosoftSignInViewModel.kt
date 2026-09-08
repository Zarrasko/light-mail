package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.MicrosoftOAuthClient
import com.thelightphone.mail.engine.MicrosoftTokenPollResult
import com.thelightphone.mail.engine.MicrosoftTokens
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailMicrosoftSignInState(
    val userCode: String? = null,
    val verificationUri: String? = null,
    val errorMessage: String? = null,
    val signedIn: Boolean = false,
)

/** Outlook/Microsoft 365 always uses these; no autodiscovery needed for a Microsoft sign-in. */
private const val OUTLOOK_IMAP_HOST = "outlook.office365.com"
private const val OUTLOOK_SMTP_HOST = "smtp.office365.com"

class MailMicrosoftSignInViewModel(
    private val repository: MailAccountRepository,
) : LightViewModel<Boolean>() {

    private val _state = MutableStateFlow(MailMicrosoftSignInState())
    val state: StateFlow<MailMicrosoftSignInState> = _state.asStateFlow()

    private var started = false

    override fun onScreenShow(screen: SimpleLightScreen<Boolean>) {
        if (started) return
        started = true
        viewModelScope.launch(Dispatchers.IO) { startSignIn() }
    }

    private suspend fun startSignIn() {
        try {
            val deviceCode = microsoftOAuthClient.requestDeviceCode(MicrosoftOAuthClient.OUTLOOK_SCOPES)
            _state.value = MailMicrosoftSignInState(
                userCode = deviceCode.userCode,
                verificationUri = deviceCode.verificationUri,
            )
            pollUntilDone(deviceCode.deviceCode, deviceCode.intervalSeconds, deviceCode.expiresInSeconds)
        } catch (e: Exception) {
            _state.value = MailMicrosoftSignInState(errorMessage = e.message ?: "Couldn't start Microsoft sign-in")
        }
    }

    private suspend fun pollUntilDone(deviceCode: String, initialIntervalSeconds: Int, expiresInSeconds: Int) {
        var intervalSeconds = initialIntervalSeconds
        val deadline = System.currentTimeMillis() + expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(intervalSeconds * 1000L)
            when (val result = microsoftOAuthClient.pollForToken(deviceCode)) {
                is MicrosoftTokenPollResult.Success -> {
                    onSignedIn(result.tokens)
                    return
                }
                is MicrosoftTokenPollResult.Pending -> Unit
                // A slow_down response means we polled too often; back off per RFC 8628 3.5.
                is MicrosoftTokenPollResult.SlowDown -> intervalSeconds += 5
                is MicrosoftTokenPollResult.Failed -> {
                    _state.update { it.copy(errorMessage = result.reason) }
                    return
                }
            }
        }
        _state.update { it.copy(errorMessage = "That code expired - try again") }
    }

    private suspend fun onSignedIn(tokens: MicrosoftTokens) {
        val email = MicrosoftOAuthClient.emailFromIdToken(tokens.idToken)
        if (email.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Couldn't read the signed-in account's email") }
            return
        }

        repository.addAccount(
            MailAccount(
                email = email,
                imapHost = OUTLOOK_IMAP_HOST,
                imapPort = 993,
                smtpHost = OUTLOOK_SMTP_HOST,
                smtpPort = 587,
                smtpUseStartTls = true,
                authType = MailAuthType.MICROSOFT_OAUTH,
                microsoftRefreshToken = tokens.refreshToken,
            ),
        )
        _state.update { it.copy(signedIn = true) }
    }
}
