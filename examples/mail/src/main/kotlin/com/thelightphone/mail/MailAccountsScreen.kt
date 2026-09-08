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
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightWork
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import kotlin.time.Duration.Companion.minutes
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
import com.thelightphone.sdk.ui.lightClickable

@InitialScreen
class MailAccountsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, MailAccountsViewModel>(sealedActivity) {

    private val repository = MailAccountRepository.getInstance {
        lightContext.buildDatabase(MailDatabase::class.java, MailAccountRepository.DATABASE_NAME, MAIL_DATABASE_MIGRATIONS)
    }

    init {
        // Idempotent: ExistingPeriodicWorkPolicy.UPDATE means showing this screen again
        // (it's the initial screen, so this runs on every cold start) just confirms the
        // schedule rather than resetting or duplicating it. 15 minutes is WorkManager's
        // floor for periodic work, so mail can't be checked more often than that.
        LightWork.enqueuePeriodic(lightContext, MAIL_SYNC_JOB_KEY, repeatInterval = 15.minutes)
    }

    override val viewModelClass: Class<MailAccountsViewModel>
        get() = MailAccountsViewModel::class.java

    override fun createViewModel() = MailAccountsViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val accounts by viewModel.accounts.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Mail"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = { navigateTo(screenFactory = { MailSettingsScreen(it) }) },
                    ),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "No accounts added yet",
                            variant = LightTextVariant.Copy,
                            align = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                        )
                    }
                } else {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        accounts.forEach { account ->
                            LightText(
                                text = account.email,
                                variant = LightTextVariant.Subheading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .lightClickable {
                                        navigateTo(screenFactory = { MailInboxScreen(it, account) })
                                    }
                                    .padding(vertical = 0.75f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                LightBottomBar(
                    listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.ADD,
                            onClick = {
                                navigateTo(screenFactory = { MailAccountSetupScreen(it) }) {
                                    viewModel.reload()
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }
}
