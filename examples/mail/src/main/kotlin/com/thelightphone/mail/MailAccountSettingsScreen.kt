package com.thelightphone.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
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

class MailAccountSettingsScreen(
    sealedActivity: SealedLightActivity,
    private val account: MailAccount,
) : LightScreen<Unit, MailAccountSettingsViewModel>(sealedActivity) {

    private val accountRepository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }
    private val settingsRepository = MailSettingsRepository.from(lightContext)

    override val viewModelClass: Class<MailAccountSettingsViewModel>
        get() = MailAccountSettingsViewModel::class.java

    override fun createViewModel() =
        MailAccountSettingsViewModel(account.id, accountRepository, settingsRepository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val currentAccount by viewModel.account.collectAsState()
        val globalDefault by viewModel.globalDefault.collectAsState()
        val resolvingFolder by viewModel.resolvingFolder.collectAsState()
        val folderToggleError by viewModel.folderToggleError.collectAsState()
        val displayedAccount = currentAccount ?: account

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(Unit) }),
                        center = LightTopBarCenter.Text(displayedAccount.email),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        val currentSelection = displayedAccount.messageLimitOverride?.let { MailMessageLimit.fromCount(it) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable {
                                    navigateTo(screenFactory = {
                                        MailMessageLimitScreen(
                                            it,
                                            currentSelection = currentSelection,
                                            inheritedDefaultLabel = globalDefault.label,
                                        )
                                    }) { selected -> viewModel.setMessageLimitOverride(selected) }
                                }
                                .padding(vertical = 0.75f.gridUnitsAsDp()),
                        ) {
                            LightText(text = "Messages to show", variant = LightTextVariant.Subheading)
                            LightText(
                                text = currentSelection?.label ?: "Default (${globalDefault.label})",
                                variant = LightTextVariant.Copy,
                            )
                        }

                        ToggleRow(
                            label = "Notifications",
                            enabled = displayedAccount.notificationsEnabled,
                            onToggle = { viewModel.setNotificationsEnabled(!displayedAccount.notificationsEnabled) },
                        )

                        MailSpecialFolderType.entries.forEach { type ->
                            FolderToggleRow(
                                label = type.label,
                                state = displayedAccount.folderState(type),
                                isResolving = resolvingFolder == type,
                                onToggle = { shown -> viewModel.setFolderShown(type, shown) },
                            )
                        }
                    }

                    LightBottomBar(
                        listOf(
                            LightBarButton.Text(
                                text = "REMOVE ACCOUNT",
                                onClick = {
                                    navigateTo(screenFactory = {
                                        MailConfirmRemoveScreen(it, displayedAccount, accountRepository)
                                    }) { removed -> if (removed) goBack(Unit) }
                                },
                            ),
                        ),
                    )
                }

                folderToggleError?.let { message ->
                    LightFullscreenModal(message = message, onClose = viewModel::dismissFolderToggleError)
                }
            }
        }
    }
}

@Composable
private fun FolderToggleRow(label: String, state: MailFolderState, isResolving: Boolean, onToggle: (Boolean) -> Unit) {
    if (isResolving) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 0.75f.gridUnitsAsDp())) {
            LightText(text = label, variant = LightTextVariant.Subheading)
            LightText(text = "Checking…", variant = LightTextVariant.Copy)
        }
    } else {
        ToggleRow(label = label, enabled = state.shown, onToggle = { onToggle(!state.shown) })
    }
}
