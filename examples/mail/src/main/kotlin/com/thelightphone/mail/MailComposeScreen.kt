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

class MailComposeScreen(
    sealedActivity: SealedLightActivity,
    private val account: MailAccount,
    private val prefillTo: String = "",
    private val prefillSubject: String = "",
) : LightScreen<Unit, MailComposeViewModel>(sealedActivity) {

    private val accountRepository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    override val viewModelClass: Class<MailComposeViewModel>
        get() = MailComposeViewModel::class.java

    override fun createViewModel() = MailComposeViewModel(account, accountRepository, prefillTo, prefillSubject)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val form by viewModel.form.collectAsState()
        val isSending by viewModel.isSending.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        fun editField(title: String, currentValue: String, compact: Boolean = false, onResult: (String) -> Unit) {
            navigateTo(screenFactory = {
                MailTextEditScreen(
                    it,
                    MailFieldEditRequest(title, currentValue, useCompactTextSize = compact, useComposeStyleActions = true),
                )
            }) { result -> result?.let(onResult) }
        }

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { viewModel.cancel(onDone = { goBack(Unit) }) },
                        ),
                        center = LightTopBarCenter.Text("New Message"),
                        rightButton = LightBarButton.LightIcon(
                            icon = LightIcons.SEND,
                            onClick = { viewModel.send(onSent = { goBack(Unit) }) },
                        ),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        ComposeFieldRow("To", form.to.ifBlank { "not set" }) {
                            editField("To", form.to) { value -> viewModel.update { it.copy(to = value) } }
                        }
                        ComposeFieldRow("Subject", form.subject.ifBlank { "not set" }) {
                            editField("Subject", form.subject) { value -> viewModel.update { it.copy(subject = value) } }
                        }
                        ComposeFieldRow("Message", form.body.ifBlank { "not set" }) {
                            editField("Message", form.body, compact = true) { value ->
                                viewModel.update { it.copy(body = value) }
                            }
                        }
                    }

                    LightBottomBar(
                        listOf(
                            null,
                            LightBarButton.LightIcon(
                                icon = LightIcons.CLOSE,
                                onClick = { viewModel.cancel(onDone = { goBack(Unit) }) },
                                contentDescription = "Cancel",
                            ),
                        ),
                    )
                }

                if (isSending) {
                    LightFullscreenModal(message = "Sending…", onClose = {})
                }
                errorMessage?.let { message ->
                    LightFullscreenModal(message = message, onClose = viewModel::dismissError)
                }
            }
        }
    }
}

@Composable
private fun ComposeFieldRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Subheading)
        LightText(text = value, variant = LightTextVariant.Copy)
    }
}
