package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.mail.engine.RawMessageBuilder
import com.thelightphone.mail.engine.SmtpClient
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MailComposeForm(
    val to: String = "",
    val subject: String = "",
    val body: String = "",
)

class MailComposeViewModel(
    private val account: MailAccount,
    private val accountRepository: MailAccountRepository,
    initialTo: String = "",
    initialSubject: String = "",
) : LightViewModel<Unit>() {

    private val _form = MutableStateFlow(MailComposeForm(to = initialTo, subject = initialSubject))
    val form: StateFlow<MailComposeForm> = _form.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun update(transform: (MailComposeForm) -> MailComposeForm) {
        _form.value = transform(_form.value)
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun send(onSent: () -> Unit) {
        val current = _form.value
        if (current.to.isBlank() || current.subject.isBlank() || current.body.isBlank()) {
            _errorMessage.value = "Fill in every field"
            return
        }

        _isSending.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val client = SmtpClient(account.smtpHost, account.smtpPort, account.smtpUseStartTls)
            try {
                client.connect()
                client.authenticateFor(account, accountRepository)
                client.sendPlainTextMessage(
                    from = account.email,
                    to = current.to,
                    subject = current.subject,
                    body = current.body,
                )
                onSent()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't send message"
            } finally {
                client.quit()
                _isSending.value = false
            }
        }
    }

    /**
     * Best-effort: if this account has a Drafts folder enabled, saves the current form there
     * before returning. Skips the network for an untouched form, and never blocks on or reports
     * a save failure - cancelling should always succeed from the user's point of view.
     */
    fun cancel(onDone: () -> Unit) {
        val current = _form.value
        val isEmpty = current.to.isBlank() && current.subject.isBlank() && current.body.isBlank()
        val draftsFolderName = account.draftsFolder.takeIf { it.shown }?.resolvedName

        if (isEmpty || draftsFolderName == null) {
            onDone()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val client = ImapClient(account.imapHost, account.imapPort)
            try {
                client.connect()
                client.loginFor(account, accountRepository)
                val rawMessage = RawMessageBuilder.buildPlainText(
                    from = account.email,
                    to = current.to,
                    subject = current.subject,
                    body = current.body,
                )
                client.appendMessage(draftsFolderName, rawMessage, flags = "\\Draft")
            } catch (e: Exception) {
                // Best-effort - cancelling shouldn't get stuck behind a failed draft save.
            } finally {
                client.logout()
            }
            onDone()
        }
    }
}
