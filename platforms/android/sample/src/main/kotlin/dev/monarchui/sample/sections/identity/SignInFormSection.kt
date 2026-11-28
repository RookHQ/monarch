package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputPurpose
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.identity.signinwithrookbutton.MonarchSignInWithRookButton
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedSignInMillis = 600L

/** The catalogue page of SignInForm: a recipe of MonarchTextField, MonarchPasswordInput and buttons. */
object SignInFormSection : CataloguePage {
    override val title = "SignInForm"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var email by rememberSaveable { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var keepSignedIn by rememberSaveable { mutableStateOf(true) }
        var emailError by rememberSaveable { mutableStateOf<String?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
        var formError by rememberSaveable { mutableStateOf<String?>(null) }
        var signingIn by remember { mutableStateOf(false) }
        var note by rememberSaveable { mutableStateOf<String?>(null) }
        val signIn = {
            val address = email.trim()
            val valid = "@" in address && "." in address
            emailError = if (valid) null else "Enter an email address like name@example.com."
            passwordError = if (password.isEmpty()) "Enter your password." else null
            formError = null
            if (emailError == null && passwordError == null) {
                signingIn = true
                scope.launch {
                    delay(SimulatedSignInMillis)
                    signingIn = false
                    if (password == "correct") {
                        note = "Signed in as $address"
                    } else {
                        formError = "That email and password don’t match."
                    }
                }
            }
        }
        CatalogueCaption("The password is “correct”.")
        MonarchText(text = "Sign in", style = MonarchTextStyle.Title, heading = true)
        MonarchSignInWithRookButton(onClick = { note = "Single sign-on would start here." }, fullWidth = true)
        MonarchButton(
            text = "Sign in with a passkey",
            onClick = { note = "The Credential Manager passkey sheet would open here." },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchDivider(modifier = Modifier.weight(1f))
            MonarchText(text = "or", style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
            MonarchDivider(modifier = Modifier.weight(1f))
        }
        formError?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
        MonarchTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.semantics { contentType = ContentType.Username + ContentType.EmailAddress },
            label = "Email",
            errorText = emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )
        MonarchPasswordInput(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            errorText = passwordError,
            purpose = MonarchPasswordInputPurpose.Current,
            keyboardActions = KeyboardActions(onDone = { signIn() }),
        )
        MonarchLink(text = "Forgot password?", onClick = { note = "Password reset would open here." })
        MonarchCheckboxRow(checked = keepSignedIn, onCheckedChange = { keepSignedIn = it }, label = "Keep me signed in")
        MonarchButton(
            text = "Sign in",
            onClick = signIn,
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
            loading = signingIn,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = "New here? ")
            MonarchLink(
                text = "Create an account",
                onClick = { note = "Sign-up would open here." },
                variant = MonarchLinkVariant.Standalone,
            )
        }
        note?.let { MonarchText(text = it) }
    }
}
