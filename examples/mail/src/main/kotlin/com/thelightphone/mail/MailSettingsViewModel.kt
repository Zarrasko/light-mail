package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MailSettingsViewModel(
    private val settingsRepository: MailSettingsRepository,
) : LightViewModel<Unit>() {

    val messageLimit: StateFlow<MailMessageLimit> = settingsRepository.defaultMessageLimit
        .stateIn(viewModelScope, SharingStarted.Eagerly, MailMessageLimit.DEFAULT)

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setMessageLimit(limit: MailMessageLimit) {
        viewModelScope.launch { settingsRepository.setDefaultMessageLimit(limit) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }
}
