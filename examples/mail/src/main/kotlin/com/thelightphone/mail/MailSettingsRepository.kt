package com.thelightphone.mail

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val MESSAGE_LIMIT_KEY = intPreferencesKey("default_message_limit")
private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")

/**
 * App-wide settings, applied to every account unless it has its own override (see
 * [MailAccount.messageLimitOverride]) or, for notifications, unless the setting here is off -
 * a global "off" always wins over a per-account "on".
 */
class MailSettingsRepository(private val dataStore: DataStore<Preferences>) {

    val defaultMessageLimit: Flow<MailMessageLimit> = dataStore.data.map { prefs ->
        MailMessageLimit.fromCount(prefs[MESSAGE_LIMIT_KEY] ?: MailMessageLimit.DEFAULT.count)
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setDefaultMessageLimit(limit: MailMessageLimit) {
        dataStore.edit { it[MESSAGE_LIMIT_KEY] = limit.count }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    companion object {
        fun from(lightContext: SealedLightContext) = MailSettingsRepository(lightContext.dataStore)
    }
}
