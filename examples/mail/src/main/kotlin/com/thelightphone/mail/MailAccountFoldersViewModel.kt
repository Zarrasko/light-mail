package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Re-reads the account on every show so folder toggles changed in Settings show up here. */
class MailAccountFoldersViewModel(
    private val accountId: Long,
    private val accountRepository: MailAccountRepository,
) : LightViewModel<Unit>() {

    private val _account = MutableStateFlow<MailAccount?>(null)
    val account: StateFlow<MailAccount?> = _account.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            _account.value = accountRepository.getAccount(accountId)
        }
    }
}
