package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MailMessageViewModel(
    private val account: MailAccount,
    private val folder: MailFolder,
    private val uid: Long,
    private val accountRepository: MailAccountRepository,
) : LightViewModel<Boolean>() {

    private val _body = MutableStateFlow<String?>(null)
    val body: StateFlow<String?> = _body.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loaded = false

    override fun onScreenShow(screen: SimpleLightScreen<Boolean>) {
        if (loaded) return
        loaded = true
        viewModelScope.launch(Dispatchers.IO) {
            val client = ImapClient(account.imapHost, account.imapPort)
            try {
                client.connect()
                client.loginFor(account, accountRepository)
                client.selectFolder(folder.imapName)
                _body.value = client.fetchPlainTextBody(uid)
                // Best-effort: failing to flag the message read shouldn't surface as an error
                // when the body itself loaded fine.
                runCatching { client.markSeen(uid) }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't load message"
            } finally {
                client.logout()
                _isLoading.value = false
            }
        }
    }
}
