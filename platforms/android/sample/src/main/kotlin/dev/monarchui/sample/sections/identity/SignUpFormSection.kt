package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputPurpose
import dev.monarchui.components.forms.passwordstrengthmeter.MonarchPasswordRule
import dev.monarchui.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeter
import dev.monarchui.components.forms.termsacceptance.MonarchTermsAcceptance
import dev.monarchui.components.forms.termsacceptance.MonarchTermsDocument
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedRequestMillis = 600L

private val Documents = listOf(
    MonarchTermsDocument("Terms of Service", "https://example.com/terms", version = "2026-11-01"),
    MonarchTermsDocument("Privacy Policy", "https://example.com/privacy", version = "2026-09-15"),
)

private fun looksLikeEmail(address: String): Boolean = "@" in address && "." in address

private fun meetsRules(password: String, rules: List<MonarchPasswordRule>): Boolean = rules.all { it.test(password) }

/** The catalogue page of SignUpForm: a recipe of text fields, the password meter, terms and a button. */
object SignUpFormSection : CataloguePage {
    override val title = "SignUpForm"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val rules = MonarchPasswordRule.defaults()
        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var accepted by rememberSaveable { mutableStateOf(false) }
        var tried by rememberSaveable { mutableStateOf(false) }
        var emailTaken by rememberSaveable { mutableStateOf<String?>(null) }
        var formError by rememberSaveable { mutableStateOf<String?>(null) }
        var creating by remember { mutableStateOf(false) }
        var note by rememberSaveable { mutableStateOf<String?>(null) }
        val address = email.trim()
        val nameError = "Enter your name.".takeIf { tried && name.isBlank() }
        val emailError = emailTaken ?: "Enter an email address like name@example.com."
            .takeIf { tried && !looksLikeEmail(address) }
        val passwordError = "Choose a password that meets every rule."
            .takeIf { tried && !meetsRules(password, rules) }
        val createAccount = {
            tried = true
            emailTaken = null
            formError = null
            val valid = name.isNotBlank() && looksLikeEmail(address) && meetsRules(password, rules)
            if (valid && accepted) {
                creating = true
                scope.launch {
                    delay(SimulatedRequestMillis)
                    creating = false
                    if (address.startsWith("taken@")) {
                        emailTaken = "An account with this email already exists."
                    } else if (address.startsWith("fail@")) {
                        formError = "Couldn’t create the account. Try again."
                    } else {
                        note = "Created the account for $address"
                    }
                }
            }
        }
        CatalogueCaption("An email starting with taken@ is in use; fail@ shows a server error")
        MonarchText(text = "Create your account", style = MonarchTextStyle.Title, heading = true)
        formError?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
        MonarchTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.semantics { contentType = ContentType.PersonFullName },
            label = "Full name",
            errorText = nameError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        MonarchTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.semantics { contentType = ContentType.NewUsername + ContentType.EmailAddress },
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
            purpose = MonarchPasswordInputPurpose.New,
        )
        MonarchPasswordStrengthMeter(password = password, rules = rules)
        MonarchTermsAcceptance(
            accepted = accepted,
            onAcceptedChange = { value, _ -> accepted = value },
            documents = Documents,
            showRequiredError = tried,
        )
        MonarchButton(
            text = "Create account",
            onClick = createAccount,
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
            loading = creating,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = "Already have an account? ")
            MonarchLink(
                text = "Sign in",
                onClick = { note = "Sign-in would open here." },
                variant = MonarchLinkVariant.Standalone,
            )
        }
        note?.let { MonarchText(text = it) }
    }
}
