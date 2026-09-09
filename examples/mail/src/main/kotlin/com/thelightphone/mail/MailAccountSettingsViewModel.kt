package com.thelightphone.mail

import androidx.lifecycle.viewModelScope
import com.thelightphone.mail.engine.ImapClient
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

    private val _resolvingFolder = MutableStateFlow<MailSpecialFolderType?>(null)
    val resolvingFolder: StateFlow<MailSpecialFolderType?> = _resolvingFolder.asStateFlow()

    private val _folderToggleError = MutableStateFlow<String?>(null)
    val folderToggleError: StateFlow<String?> = _folderToggleError.asStateFlow()

    fun dismissFolderToggleError() {
        _folderToggleError.value = null
    }

    /**
     * Turning a folder on resolves its provider-specific IMAP name the first time (see
     * [ImapClient.resolveSpecialUseFolder]) and caches it; later toggles just flip
     * [MailFolderState.shown]. Turning one off clears the cached name rather than just hiding
     * it, so if the resolved folder ever goes stale (renamed or deleted on the server), turning
     * it off and back on re-resolves it instead of repeating the same failure.
     */
    fun setFolderShown(type: MailSpecialFolderType, shown: Boolean) {
        val current = _account.value ?: return
        val existingState = current.folderState(type)

        if (!shown) {
            viewModelScope.launch(Dispatchers.IO) {
                persistFolderState(type, MailFolderState(shown = false, resolvedName = null))
            }
            return
        }

        if (existingState.resolvedName != null) {
            viewModelScope.launch(Dispatchers.IO) {
                persistFolderState(type, existingState.copy(shown = true))
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _resolvingFolder.value = type
            _folderToggleError.value = null
            val client = ImapClient(current.imapHost, current.imapPort)
            try {
                client.connect()
                client.loginFor(current, accountRepository)
                val resolved = client.resolveSpecialUseFolder(type.specialUseFlag, type.candidates)
                if (resolved != null) {
                    persistFolderState(type, MailFolderState(shown = true, resolvedName = resolved))
                } else {
                    _folderToggleError.value = "Couldn't find a ${type.label} folder for this account"
                }
            } catch (e: Exception) {
                _folderToggleError.value = e.message ?: "Couldn't check for a ${type.label} folder"
            } finally {
                client.logout()
                _resolvingFolder.value = null
            }
        }
    }

    private suspend fun persistFolderState(type: MailSpecialFolderType, state: MailFolderState) {
        when (type) {
            MailSpecialFolderType.SENT -> accountRepository.updateSentFolder(accountId, state)
            MailSpecialFolderType.DRAFTS -> accountRepository.updateDraftsFolder(accountId, state)
            MailSpecialFolderType.TRASH -> accountRepository.updateTrashFolder(accountId, state)
        }
        _account.value = accountRepository.getAccount(accountId)
    }
}
