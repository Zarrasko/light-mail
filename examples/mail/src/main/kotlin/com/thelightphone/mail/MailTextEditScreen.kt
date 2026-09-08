package com.thelightphone.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightInputTextStyle

data class MailFieldEditRequest(
    val title: String,
    val initialValue: String,
    /** Long-form fields (the message body) use a smaller style so more text fits on screen. */
    val useCompactTextSize: Boolean = false,
)

/** A single full-screen field editor, used by account setup and compose for one field at a time. */
class MailTextEditScreen(
    sealedActivity: SealedLightActivity,
    private val request: MailFieldEditRequest,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val keyboardOptionsFlow = rememberKeyboardOptions()
        val textState = rememberTextFieldState(request.initialValue)
        val themeColors by LightThemeController.colors.collectAsState()
        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = request.title,
                state = textState,
                keyboardOptionsFlow = keyboardOptionsFlow,
                onSubmit = { result -> goBack(result.toString()) },
                onBack = { goBack(null) },
                modifier = Modifier.background(LightThemeTokens.colors.background),
                textStyle = if (request.useCompactTextSize) {
                    lightInputTextStyle(base = LightThemeTokens.typography.paragraph)
                } else {
                    lightInputTextStyle()
                },
            )
        }
    }
}
