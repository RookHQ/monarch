package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.passwordinput.MonarchPasswordInput
import com.rookhq.monarch.components.forms.passwordinput.MonarchPasswordInputPurpose
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordRule
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeter
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedRequestMillis = 600L

/** The steps of the recipe. */
private enum class ResetStep { Request, Sent, Reset, Done }

/** The catalogue page of PasswordResetForm: a recipe of MonarchTextField, MonarchPasswordInput and the meter. */
object PasswordResetFormSection : CataloguePage {
    override val title = "PasswordResetForm"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var step by rememberSaveable { mutableStateOf(ResetStep.Request) }
        var email by rememberSaveable { mutableStateOf("") }
        var emailError by rememberSaveable { mutableStateOf<String?>(null) }
        var password by remember { mutableStateOf("") }
        var confirmation by remember { mutableStateOf("") }
        var working by remember { mutableStateOf(false) }
        val rules = MonarchPasswordRule.defaults()
        val mismatch = if (confirmation.isEmpty() || confirmation == password) null else "The passwords don’t match."
        CatalogueCaption("In an app, the emailed App Link opens the reset step.")
        when (step) {
            ResetStep.Request -> {
                Title("Reset your password")
                MonarchText(text = "Enter the email address of your account and we’ll send you a link.")
                MonarchTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "name@example.com",
                    errorText = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                )
                MonarchButton(
                    text = "Send reset link",
                    onClick = {
                        if ("@" !in email || "." !in email) {
                            emailError = "Enter an email address like name@example.com."
                        } else {
                            emailError = null
                            working = true
                            scope.launch {
                                delay(SimulatedRequestMillis)
                                working = false
                                step = ResetStep.Sent
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Primary,
                    loading = working,
                )
            }
            ResetStep.Sent -> {
                Title("Check your email")
                MonarchText(text = "If an account exists for $email, you’ll get an email with a link.")
                MonarchButton(text = "Open the link (simulated)", onClick = { step = ResetStep.Reset })
                MonarchButton(
                    text = "Use a different email",
                    onClick = { step = ResetStep.Request },
                    variant = MonarchButtonVariant.Ghost,
                )
            }
            ResetStep.Reset -> {
                Title("Choose a new password")
                MonarchPasswordInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "New password",
                    purpose = MonarchPasswordInputPurpose.New,
                )
                MonarchPasswordStrengthMeter(password = password, rules = rules)
                MonarchPasswordInput(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = "Confirm new password",
                    errorText = mismatch,
                    purpose = MonarchPasswordInputPurpose.New,
                )
                MonarchButton(
                    text = "Change password",
                    onClick = { step = ResetStep.Done },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Primary,
                    enabled = rules.all { it.test(password) } && confirmation.isNotEmpty() && mismatch == null,
                )
            }
            ResetStep.Done -> {
                Title("Password changed")
                MonarchText(text = "You can now sign in with your new password.")
                MonarchButton(
                    text = "Back to sign in",
                    onClick = {
                        password = ""
                        confirmation = ""
                        step = ResetStep.Request
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Primary,
                )
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    MonarchText(text = text, modifier = Modifier.semantics { heading() }, style = MonarchTextStyle.Title)
}
