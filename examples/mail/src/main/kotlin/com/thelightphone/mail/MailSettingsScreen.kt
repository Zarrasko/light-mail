package com.thelightphone.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
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

class MailSettingsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, MailSettingsViewModel>(sealedActivity) {

    override val viewModelClass: Class<MailSettingsViewModel>
        get() = MailSettingsViewModel::class.java

    override fun createViewModel() = MailSettingsViewModel(MailSettingsRepository.from(lightContext))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val messageLimit by viewModel.messageLimit.collectAsState()
        val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(Unit) }),
                    center = LightTopBarCenter.Text("Settings"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable {
                                navigateTo(screenFactory = {
                                    MailMessageLimitScreen(it, currentSelection = messageLimit)
                                }) { selected -> selected?.let(viewModel::setMessageLimit) }
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                    ) {
                        LightText(text = "Messages to show", variant = LightTextVariant.Subheading)
                        LightText(text = messageLimit.label, variant = LightTextVariant.Copy)
                    }

                    ToggleRow(
                        label = "Notifications",
                        enabled = notificationsEnabled,
                        onToggle = { viewModel.setNotificationsEnabled(!notificationsEnabled) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToggleRow(label: String, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onToggle)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(text = label, variant = LightTextVariant.Subheading)
        LightIcon(icon = if (enabled) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF)
    }
}
