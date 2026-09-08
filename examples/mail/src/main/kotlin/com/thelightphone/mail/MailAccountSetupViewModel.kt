package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.ServerAutodiscovery
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailAccountForm(
    val email: String = "",
    val password: String = "",
    val imapHost: String = "",
    val imapPort: String = "993",
    val smtpHost: String = "",
    val smtpPort: String = "465",
    val isLookingUpServerSettings: Boolean = false,
)

class MailAccountSetupViewModel(
    private val repository: MailAccountRepository,
) : LightViewModel<Boolean>() {

    private val _form = MutableStateFlow(MailAccountForm())
    val form: StateFlow<MailAccountForm> = _form.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun update(transform: (MailAccountForm) -> MailAccountForm) {
        _form.value = transform(_form.value)
    }

    /**
     * Sets the email address and, if the host fields haven't been touched yet, looks up its
     * IMAP/SMTP settings automatically (a known-provider table, then a network fallback) so
     * people can set up an account with just an email + password. Manual entry still works -
     * the host/port fields stay editable, and lookup silently does nothing if it finds no match.
     */
    fun updateEmail(email: String) {
        _form.update { it.copy(email = email) }

        val hostsAlreadySet = _form.value.imapHost.isNotBlank() || _form.value.smtpHost.isNotBlank()
        if (hostsAlreadySet || email.substringAfterLast('@', "").isBlank()) return

        _form.update { it.copy(isLookingUpServerSettings = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val config = ServerAutodiscovery.lookup(email)
            _form.update { current ->
                val stillSameEmail = current.email == email
                val stillUntouched = current.imapHost.isBlank() && current.smtpHost.isBlank()
                if (config != null && stillSameEmail && stillUntouched) {
                    current.copy(
                        imapHost = config.imapHost,
                        imapPort = config.imapPort.toString(),
                        smtpHost = config.smtpHost,
                        smtpPort = config.smtpPort.toString(),
                        isLookingUpServerSettings = false,
                    )
                } else {
                    current.copy(isLookingUpServerSettings = false)
                }
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    /** Validates and saves the form, invoking [onSaved] with true on success. */
    fun save(onSaved: (Boolean) -> Unit) {
        val current = _form.value
        val imapPort = current.imapPort.toIntOrNull()
        val smtpPort = current.smtpPort.toIntOrNull()

        if (current.email.isBlank() || current.password.isBlank() ||
            current.imapHost.isBlank() || current.smtpHost.isBlank()
        ) {
            _errorMessage.value = "Fill in every field"
            return
        }
        if (imapPort == null || smtpPort == null) {
            _errorMessage.value = "Ports must be numbers"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.addAccount(
                MailAccount(
                    email = current.email,
                    password = current.password,
                    imapHost = current.imapHost,
                    imapPort = imapPort,
                    smtpHost = current.smtpHost,
                    smtpPort = smtpPort,
                    smtpUseStartTls = smtpPort == 587,
                ),
            )
            onSaved(true)
        }
    }
}
