package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardGroup
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInput
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityResult
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedLookupMillis = 500L
private const val SimulatedCreateMillis = 800L
private val NameLength = 4..25
private val UsernameLength = 5..30

/** The username rules of the example, then a simulated lookup in which "acmeinc" is taken. */
private suspend fun checkUsername(handle: String): MonarchUsernameAvailabilityResult = when {
    handle.any { !it.isLetterOrDigit() || it.code > Byte.MAX_VALUE } ->
        MonarchUsernameAvailabilityResult(valid = false, message = "Use letters and numbers only")
    handle.length !in UsernameLength ->
        MonarchUsernameAvailabilityResult(valid = false, message = "Use 5 to 30 characters")
    else -> {
        delay(SimulatedLookupMillis)
        MonarchUsernameAvailabilityResult(valid = handle.lowercase() != "acmeinc")
    }
}

/** The error of the name, once the person has typed or submitted. */
private fun nameError(length: Int, submitted: Boolean): String? = when {
    !submitted && length == 0 -> null
    length < NameLength.first -> "Name must be at least 4 characters"
    length > NameLength.last -> "Name must be at most 25 characters"
    else -> null
}

/** The catalogue page of WorkspaceOnboardingForm: a recipe of a field, a username check, cards and a button. */
object WorkspaceOnboardingFormSection : CataloguePage {
    override val title = "WorkspaceOnboardingForm"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var name by rememberSaveable { mutableStateOf("") }
        var username by rememberSaveable { mutableStateOf("") }
        var type by rememberSaveable { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf(MonarchUsernameAvailabilityStatus.Idle) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        var creating by remember { mutableStateOf(false) }
        var created by rememberSaveable { mutableStateOf(false) }
        val length = name.trim().length
        val nameError = nameError(length, submitted)
        CatalogueCaption("\"acmeinc\" is taken. Continue waits for the username check.")
        if (created) MonarchInlineNotice(message = "Workspace @$username created.", tone = MonarchTone.Success)
        MonarchTextField(
            value = name,
            onValueChange = { name = it },
            label = "Workspace name",
            placeholder = "Acme Inc.",
            errorText = nameError,
        )
        MonarchUsernameAvailabilityInput(
            value = username,
            label = "Workspace username",
            onValueChange = { username = it },
            validate = ::checkUsername,
            placeholder = "acme",
            helperText = "Letters and numbers only, 5 to 30 characters.",
            prefix = "@",
            onStatusChange = { newStatus, _ -> status = newStatus },
        )
        MonarchRadioCardGroup(
            options = listOf(
                MonarchRadioCardOption("creator", title = "Creator", description = "For personal projects"),
                MonarchRadioCardOption("business", title = "Business", description = "For teams and companies"),
            ),
            selected = type,
            onSelect = { type = it },
            label = "Type",
            columns = 2,
        )
        if (submitted && type == null) {
            MonarchText(
                text = "Choose a workspace type",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.dangerStrong,
            )
        }
        if (submitted && status == MonarchUsernameAvailabilityStatus.Checking) {
            MonarchText(text = "Wait for the username check to finish.", style = MonarchTextStyle.Caption)
        }
        MonarchButton(
            text = "Continue",
            onClick = {
                submitted = true
                val ready = nameError == null && length > 0 && type != null
                if (ready && status == MonarchUsernameAvailabilityStatus.Valid) {
                    creating = true
                    scope.launch {
                        delay(SimulatedCreateMillis)
                        creating = false
                        created = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
            loading = creating,
        )
    }
}
