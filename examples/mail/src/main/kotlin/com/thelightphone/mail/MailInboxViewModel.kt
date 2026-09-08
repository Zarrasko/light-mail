package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.mail.engine.ImapMessageSummary
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MailInboxViewModel(
    private val account: MailAccount,
) : LightViewModel<Unit>() {

    private val _messages = MutableStateFlow<List<ImapMessageSummary>>(emptyList())
    val messages: StateFlow<List<ImapMessageSummary>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) { refreshInbox() }
    }

    private suspend fun refreshInbox() {
        _isLoading.value = true
        _errorMessage.value = null
        val client = ImapClient(account.imapHost, account.imapPort)
        try {
            client.connect()
            client.login(account.email, account.password)
            val messageCount = client.selectInbox()
            _messages.value = if (messageCount > 0) {
                val start = maxOf(1, messageCount - MAX_MESSAGES_SHOWN + 1)
                client.fetchSummaries("$start:$messageCount").sortedByDescending { it.uid }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Couldn't load inbox"
        } finally {
            client.logout()
            _isLoading.value = false
        }
    }

    companion object {
        private const val MAX_MESSAGES_SHOWN = 20
    }
}
