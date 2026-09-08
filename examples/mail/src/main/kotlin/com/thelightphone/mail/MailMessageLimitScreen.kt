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
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
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

/**
 * A pick-one list of [MailMessageLimit]s. When [inheritedDefaultLabel] is non-null, an extra
 * "Use default" row is shown that returns null (clearing an account's override); otherwise
 * every row is a concrete choice, for editing the app-wide default itself.
 */
class MailMessageLimitScreen(
    sealedActivity: SealedLightActivity,
    private val currentSelection: MailMessageLimit?,
    private val inheritedDefaultLabel: String? = null,
) : SimpleLightScreen<MailMessageLimit?>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack(currentSelection) }),
                    center = LightTopBarCenter.Text("Messages to show"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    if (inheritedDefaultLabel != null) {
                        LimitRow(
                            label = "Use default ($inheritedDefaultLabel)",
                            selected = currentSelection == null,
                            onClick = { goBack(null) },
                        )
                    }
                    MailMessageLimit.entries.forEach { option ->
                        LimitRow(
                            label = option.label,
                            selected = currentSelection == option,
                            onClick = { goBack(option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitRow(label: String, selected: Boolean, onClick: () -> Unit) {
    LightText(
        text = if (selected) "• $label" else label,
        variant = if (selected) LightTextVariant.Subheading else LightTextVariant.Copy,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    )
}
