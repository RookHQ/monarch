package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of AuthPage, a recipe. In an app the column fills the
 * screen, scrolls with verticalScroll and keeps clear of the keyboard with
 * imePadding; the catalogue page already scrolls, so it shows the column only.
 */
object AuthPageSection : CataloguePage {
    override val title = "AuthPage"

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var signingIn by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A sign-in screen")
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space6),
        ) {
            MonarchCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
                    MonarchText(text = "Sign in", style = MonarchTextStyle.Title, heading = true)
                    MonarchText(
                        text = "Use the email address of your work account.",
                        color = MonarchTheme.colors.textMuted,
                    )
                    MonarchInlineNotice(
                        message = "Your session expired. Sign in again to continue.",
                        tone = MonarchTone.Warning,
                    )
                    MonarchTextField(value = email, onValueChange = { email = it }, label = "Email")
                    MonarchPasswordInput(value = password, onValueChange = { password = it }, label = "Password")
                    MonarchButton(
                        text = if (signingIn) "Signed in" else "Sign in",
                        onClick = { signingIn = !signingIn },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                    MonarchLink(text = "Forgot your password?", onClick = {}, variant = MonarchLinkVariant.Standalone)
                }
            }
            MonarchText(text = "New to Rook? Ask your administrator for an invitation.", textAlign = TextAlign.Center)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4, Alignment.CenterHorizontally),
            ) {
                MonarchLink(text = "Terms", onClick = {}, variant = MonarchLinkVariant.Subtle)
                MonarchLink(text = "Privacy", onClick = {}, variant = MonarchLinkVariant.Subtle)
                MonarchLink(text = "Help", onClick = {}, variant = MonarchLinkVariant.Subtle)
            }
        }
    }
}
