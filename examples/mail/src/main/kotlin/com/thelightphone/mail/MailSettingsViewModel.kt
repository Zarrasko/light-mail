package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _updateCheckMessage = MutableStateFlow<String?>(null)
    val updateCheckMessage: StateFlow<String?> = _updateCheckMessage.asStateFlow()

    /**
     * Read-only: reports whether a newer version exists on GitHub, but can't download or
     * install it - the SDK's sandbox doesn't let a Tool launch the system installer.
     */
    fun checkForUpdate() {
        if (_isCheckingForUpdate.value) return
        _isCheckingForUpdate.value = true
        viewModelScope.launch(Dispatchers.IO) {
            _updateCheckMessage.value = try {
                val result = MailUpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                if (result.isUpdateAvailable) {
                    "Version ${result.latestVersion} is available (you have ${result.currentVersion}) - " +
                        "download it from the Releases page on GitHub."
                } else {
                    "You're up to date (v${result.currentVersion})."
                }
            } catch (e: Exception) {
                e.message ?: "Couldn't check for updates"
            }
            _isCheckingForUpdate.value = false
        }
    }

    fun dismissUpdateCheckMessage() {
        _updateCheckMessage.value = null
    }
}
