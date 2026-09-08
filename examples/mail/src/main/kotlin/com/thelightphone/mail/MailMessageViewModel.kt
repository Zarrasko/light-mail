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
    private val uid: Long,
) : LightViewModel<Unit>() {

    private val _body = MutableStateFlow<String?>(null)
    val body: StateFlow<String?> = _body.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loaded = false

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        if (loaded) return
        loaded = true
        viewModelScope.launch(Dispatchers.IO) {
            val client = ImapClient(account.imapHost, account.imapPort)
            try {
                client.connect()
                client.login(account.email, account.password)
                client.selectInbox()
                _body.value = client.fetchPlainTextBody(uid)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't load message"
            } finally {
                client.logout()
                _isLoading.value = false
            }
        }
    }
}
