package com.thelightphone.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

class MailMicrosoftSignInScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Boolean, MailMicrosoftSignInViewModel>(sealedActivity) {

    private val repository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    override val viewModelClass: Class<MailMicrosoftSignInViewModel>
        get() = MailMicrosoftSignInViewModel::class.java

    override fun createViewModel() = MailMicrosoftSignInViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.signedIn) {
            if (state.signedIn) goBack(true)
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(false) }),
                    center = LightTopBarCenter.Text("Sign in with Microsoft"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.errorMessage != null -> LightText(
                            text = state.errorMessage.orEmpty(),
                            variant = LightTextVariant.Copy,
                            align = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.userCode != null -> Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LightText(
                                text = "On another device, go to",
                                variant = LightTextVariant.Copy,
                                align = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            LightText(
                                text = state.verificationUri.orEmpty(),
                                variant = LightTextVariant.Subheading,
                                align = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                            LightText(
                                text = "and enter this code:",
                                variant = LightTextVariant.Copy,
                                align = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            LightText(
                                text = state.userCode.orEmpty(),
                                variant = LightTextVariant.Heading,
                                align = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 0.5f.gridUnitsAsDp()),
                            )
                        }
                        else -> LightText(text = "Connecting…", variant = LightTextVariant.Copy)
                    }
                }
            }
        }
    }
}
