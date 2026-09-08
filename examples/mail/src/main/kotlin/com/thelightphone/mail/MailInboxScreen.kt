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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.mail.engine.ImapMessageSummary
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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

class MailInboxScreen(
    sealedActivity: SealedLightActivity,
    private val account: MailAccount,
) : LightScreen<Unit, MailInboxViewModel>(sealedActivity) {

    override val viewModelClass: Class<MailInboxViewModel>
        get() = MailInboxViewModel::class.java

    override fun createViewModel() = MailInboxViewModel(account, MailSettingsRepository.from(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val messages by viewModel.messages.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
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

                    if (isLoading && messages.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(text = "Loading…", variant = LightTextVariant.Copy)
                        }
                    } else if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(text = "No messages", variant = LightTextVariant.Copy)
                        }
                    } else {
                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                        ) {
                            messages.forEach { summary ->
                                MessageRow(
                                    summary = summary,
                                    modifier = Modifier
                                        .lightClickable {
                                            navigateTo(screenFactory = {
                                                MailMessageScreen(it, account, summary)
                                            }) { deleted -> if (deleted) viewModel.reload() }
                                        }
                                        .padding(vertical = 0.75f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }

                    LightBottomBar(
                        listOf(
                            LightBarButton.LightIcon(
                                icon = LightIcons.REFRESH,
                                onClick = { viewModel.reload() },
                                contentDescription = "Refresh",
                            ),
                            LightBarButton.LightIcon(
                                icon = LightIcons.PENCIL,
                                onClick = {
                                    navigateTo(screenFactory = { MailComposeScreen(it, account) })
                                },
                            ),
                        ),
                    )
                }

                errorMessage?.let { message ->
                    LightFullscreenModal(message = message, onClose = viewModel::dismissError)
                }
            }
        }
    }
}

@Composable
private fun MessageRow(summary: ImapMessageSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        LightText(
            text = summary.from.ifBlank { "(unknown sender)" },
            variant = if (summary.isSeen) LightTextVariant.Copy else LightTextVariant.Subheading,
        )
        LightText(
            text = summary.subject.ifBlank { "(no subject)" },
            variant = LightTextVariant.Detail,
        )
    }
}
