package com.thelightphone.mail

import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.mail.engine.MicrosoftOAuthClient
import com.thelightphone.mail.engine.SmtpClient

/**
 * Client ID for Mail's own Microsoft Entra ID app registration (multi-tenant + personal
 * accounts, public client flows enabled). This lets anyone running this app sign in to their
 * own Outlook/Microsoft 365 account without registering an app of their own.
 */
internal const val MICROSOFT_CLIENT_ID = "69533079-4e11-4644-b71c-60762593cf5d"

internal val microsoftOAuthClient = MicrosoftOAuthClient(MICROSOFT_CLIENT_ID)

/**
 * Logs an already-connected [ImapClient] in for [account], refreshing its Microsoft access
 * token first for OAuth accounts - access tokens are short-lived and never persisted, only the
 * refresh token is. The rotated refresh token Microsoft returns on every refresh is written
 * back through [repository] so the next sign-in doesn't need the user's involvement.
 */
suspend fun ImapClient.loginFor(account: MailAccount, repository: MailAccountRepository) {
    when (account.authType) {
        MailAuthType.PASSWORD -> login(account.email, account.password)
        MailAuthType.MICROSOFT_OAUTH -> loginXOAuth2(account.email, freshMicrosoftAccessToken(account, repository))
    }
}

/** SMTP counterpart of [ImapClient.loginFor]. */
suspend fun SmtpClient.authenticateFor(account: MailAccount, repository: MailAccountRepository) {
    when (account.authType) {
        MailAuthType.PASSWORD -> authenticatePlain(account.email, account.password)
        MailAuthType.MICROSOFT_OAUTH ->
            authenticateXOAuth2(account.email, freshMicrosoftAccessToken(account, repository))
    }
}

private suspend fun freshMicrosoftAccessToken(account: MailAccount, repository: MailAccountRepository): String {
    val tokens = microsoftOAuthClient.refreshTokens(account.microsoftRefreshToken, MicrosoftOAuthClient.OUTLOOK_SCOPES)
    repository.updateMicrosoftRefreshToken(account.id, tokens.refreshToken)
    return tokens.accessToken
}
