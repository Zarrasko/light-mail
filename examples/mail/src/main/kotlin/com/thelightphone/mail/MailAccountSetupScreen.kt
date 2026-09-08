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

class MailAccountSetupScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Boolean, MailAccountSetupViewModel>(sealedActivity) {

    private val repository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    override val viewModelClass: Class<MailAccountSetupViewModel>
        get() = MailAccountSetupViewModel::class.java

    override fun createViewModel() = MailAccountSetupViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val form by viewModel.form.collectAsState()
        val errorMessage by viewModel.errorMessage.collectAsState()

        fun editField(title: String, currentValue: String, onResult: (String) -> Unit) {
            navigateTo(screenFactory = {
                MailTextEditScreen(it, MailFieldEditRequest(title, currentValue))
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
                        leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(false) }),
                        center = LightTopBarCenter.Text("Add Account"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        SetupFieldRow("Email", form.email.ifBlank { "not set" }) {
                            editField("Email", form.email) { value -> viewModel.updateEmail(value) }
                        }
                        SetupFieldRow("Password", if (form.password.isBlank()) "not set" else "••••••••") {
                            editField("Password", form.password) { value -> viewModel.update { it.copy(password = value) } }
                        }
                        val hostPlaceholder = if (form.isLookingUpServerSettings) "looking up…" else "not set"
                        SetupFieldRow("IMAP host", form.imapHost.ifBlank { hostPlaceholder }) {
                            editField("IMAP host", form.imapHost) { value -> viewModel.update { it.copy(imapHost = value) } }
                        }
                        SetupFieldRow("IMAP port", form.imapPort) {
                            editField("IMAP port", form.imapPort) { value -> viewModel.update { it.copy(imapPort = value) } }
                        }
                        SetupFieldRow("SMTP host", form.smtpHost.ifBlank { hostPlaceholder }) {
                            editField("SMTP host", form.smtpHost) { value -> viewModel.update { it.copy(smtpHost = value) } }
                        }
                        SetupFieldRow("SMTP port", form.smtpPort) {
                            editField("SMTP port", form.smtpPort) { value -> viewModel.update { it.copy(smtpPort = value) } }
                        }
                    }

                    LightBottomBar(
                        listOf(
                            LightBarButton.Text(
                                text = "SAVE",
                                onClick = { viewModel.save { saved -> goBack(saved) } },
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
private fun SetupFieldRow(label: String, value: String, onClick: () -> Unit) {
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
