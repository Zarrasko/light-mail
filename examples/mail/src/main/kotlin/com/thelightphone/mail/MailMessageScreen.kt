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
import com.thelightphone.mail.engine.ImapMessageSummary
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
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

class MailMessageScreen(
    sealedActivity: SealedLightActivity,
    private val account: MailAccount,
    private val folder: MailFolder,
    private val summary: ImapMessageSummary,
) : LightScreen<Boolean, MailMessageViewModel>(sealedActivity) {

    private val accountRepository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    override val viewModelClass: Class<MailMessageViewModel>
        get() = MailMessageViewModel::class.java

    override fun createViewModel() = MailMessageViewModel(account, folder, summary.uid, accountRepository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val body by viewModel.body.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(false) }),
                    center = LightTopBarCenter.Text(summary.from.ifBlank { "Message" }),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    LightText(
                        text = summary.subject.ifBlank { "(no subject)" },
                        variant = LightTextVariant.Heading,
                        modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                    )
                    LightText(
                        text = summary.date,
                        variant = LightTextVariant.Detail,
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    when {
                        isLoading -> LightText(text = "Loading…", variant = LightTextVariant.Copy)
                        errorMessage != null -> LightText(
                            text = errorMessage.orEmpty(),
                            variant = LightTextVariant.Copy,
                        )
                        else -> LightText(
                            text = body.orEmpty(),
                            variant = LightTextVariant.Paragraph,
                        )
                    }
                }

                LightBottomBar(
                    listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.TRASH,
                            onClick = {
                                navigateTo(screenFactory = {
                                    MailConfirmDeleteMessageScreen(it, account, folder, summary)
                                }) { deleted -> if (deleted) goBack(true) }
                            },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.COMPOSE_MESSAGE,
                            onClick = {
                                navigateTo(screenFactory = {
                                    MailComposeScreen(
                                        it,
                                        account = account,
                                        prefillTo = summary.from,
                                        prefillSubject = "Re: ${summary.subject}",
                                    )
                                })
                            },
                        ),
                    ),
                )
            }
        }
    }
}
