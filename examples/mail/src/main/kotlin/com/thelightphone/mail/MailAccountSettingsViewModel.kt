package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MailAccountSettingsViewModel(
    private val accountId: Long,
    private val accountRepository: MailAccountRepository,
    private val settingsRepository: MailSettingsRepository,
) : LightViewModel<Unit>() {

    private val _account = MutableStateFlow<MailAccount?>(null)
    val account: StateFlow<MailAccount?> = _account.asStateFlow()

    val globalDefault: StateFlow<MailMessageLimit> = settingsRepository.defaultMessageLimit
        .stateIn(viewModelScope, SharingStarted.Eagerly, MailMessageLimit.DEFAULT)

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            _account.value = accountRepository.getAccount(accountId)
        }
    }

    /** Pass null to clear the override and inherit the app-wide default. */
    fun setMessageLimitOverride(limit: MailMessageLimit?) {
        viewModelScope.launch(Dispatchers.IO) {
            accountRepository.updateMessageLimitOverride(accountId, limit?.count)
            _account.value = accountRepository.getAccount(accountId)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            accountRepository.updateNotificationsEnabled(accountId, enabled)
            _account.value = accountRepository.getAccount(accountId)
        }
    }
}
