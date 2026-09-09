package com.thelightphone.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

/** Lists an account's Inbox plus any special-use folders enabled in Settings. */
class MailAccountFoldersScreen(
    sealedActivity: SealedLightActivity,
    private val initialAccount: MailAccount,
) : LightScreen<Unit, MailAccountFoldersViewModel>(sealedActivity) {

    private val accountRepository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    override val viewModelClass: Class<MailAccountFoldersViewModel>
        get() = MailAccountFoldersViewModel::class.java

    override fun createViewModel() = MailAccountFoldersViewModel(initialAccount.id, accountRepository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val currentAccount by viewModel.account.collectAsState()
        val account = currentAccount ?: initialAccount
        val folders = listOf(MailFolder.INBOX) + account.extraFolders()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(Unit) }),
                    center = LightTopBarCenter.Text(account.email),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = {
                            navigateTo(screenFactory = { MailAccountSettingsScreen(it, account) })
                        },
                    ),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    folders.forEach { folder ->
                        LightText(
                            text = folder.label,
                            variant = LightTextVariant.Subheading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    navigateTo(screenFactory = { MailInboxScreen(it, account, folder) })
                                }
                                .padding(vertical = 0.75f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
    }
}
