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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.mail.engine.ImapClient
import com.thelightphone.mail.engine.ImapMessageSummary
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MailConfirmDeleteMessageScreen(
    sealedActivity: SealedLightActivity,
    private val account: MailAccount,
    private val summary: ImapMessageSummary,
) : SimpleLightScreen<Boolean>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        var isDeleting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(false) }),
                        center = LightTopBarCenter.Text(summary.from.ifBlank { "Delete message" }),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "Delete this message? It can't be undone from here.",
                            variant = LightTextVariant.Copy,
                            align = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "CONFIRM",
                                onClick = {
                                    scope.launch {
                                        isDeleting = true
                                        errorMessage = null
                                        val client = ImapClient(account.imapHost, account.imapPort)
                                        try {
                                            withContext(Dispatchers.IO) {
                                                client.connect()
                                                client.login(account.email, account.password)
                                                client.selectInbox()
                                                client.deleteMessage(summary.uid)
                                            }
                                            goBack(true)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Couldn't delete message"
                                        } finally {
                                            withContext(Dispatchers.IO) { client.logout() }
                                            isDeleting = false
                                        }
                                    }
                                },
                            ),
                        ),
                    )
                }

                if (isDeleting) {
                    LightFullscreenModal(message = "Deleting…", onClose = {})
                }
                errorMessage?.let { message ->
                    LightFullscreenModal(message = message, onClose = { errorMessage = null })
                }
            }
        }
    }
}
