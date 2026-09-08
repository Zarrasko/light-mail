package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MailAccountsViewModel(
    private val repository: MailAccountRepository,
) : LightViewModel<Unit>() {

    private val _accounts = MutableStateFlow<List<MailAccount>>(emptyList())
    val accounts: StateFlow<List<MailAccount>> = _accounts.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            _accounts.value = repository.listAccounts()
        }
    }
}
