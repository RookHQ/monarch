@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.geo.countryselect.MonarchCountrySelect
import dev.monarchui.components.geo.countryselect.MonarchCountrySelectLabels
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DataSubjectRequestForm: a recipe of existing components. */
object DataSubjectRequestFormSection : CataloguePage {
    override val title = "DataSubjectRequestForm"

    // The page shows the recipe as one block, as in the inventory example.
    @Suppress("CyclomaticComplexMethod")
    @Composable
    override fun Content() {
        var type by rememberSaveable { mutableStateOf<String?>(null) }
        var forSelf by rememberSaveable { mutableStateOf(true) }
        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var agentName by rememberSaveable { mutableStateOf("") }
        var country by rememberSaveable { mutableStateOf<String?>(null) }
        var details by rememberSaveable { mutableStateOf("") }
        var accurate by rememberSaveable { mutableStateOf(false) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        var reference by rememberSaveable { mutableStateOf<String?>(null) }
        val emailValid = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
        val problems = buildList {
            if (type == null) add("Choose what you would like to do.")
            if (!emailValid) add("Enter an email address like name@example.com.")
            if (!forSelf && agentName.isBlank()) add("Enter your full name.")
            if (country == null) add("Choose a country or region.")
            if (type == "correct" && details.isBlank()) add("Describe what needs correcting.")
            if (!accurate) add("Confirm that the information is accurate.")
        }
        CatalogueCaption("A privacy request form; Send request validates every answer")
        val sent = reference
        if (sent != null) {
            MonarchBanner(
                title = "Request received",
                message = "Your reference is $sent. Keep it in case you need to ask about the request.",
                tone = MonarchTone.Success,
                announcement = MonarchBannerAnnouncement.Polite,
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
            ) {
                if (submitted && problems.isNotEmpty()) {
                    MonarchBanner(
                        title = if (problems.size == 1) {
                            "There is a problem with 1 answer"
                        } else {
                            "There are problems with ${problems.size} answers"
                        },
                        message = problems.joinToString(" "),
                        tone = MonarchTone.Danger,
                        announcement = MonarchBannerAnnouncement.Assertive,
                    )
                }
                MonarchRadioCardGroup(
                    options = listOf(
                        MonarchRadioCardOption(
                            "access",
                            "Get a copy of my data",
                            "Receive a copy of the personal information held about you.",
                        ),
                        MonarchRadioCardOption(
                            "delete",
                            "Delete my data",
                            "Ask for your personal information to be deleted.",
                        ),
                        MonarchRadioCardOption(
                            "correct",
                            "Correct my data",
                            "Fix personal information that is wrong or incomplete.",
                        ),
                    ),
                    selected = type,
                    onSelect = { type = it },
                    label = "What would you like to do?",
                )
                MonarchRadioCardGroup(
                    options = listOf(
                        MonarchRadioCardOption(true, "Myself"),
                        MonarchRadioCardOption(false, "Someone I am authorized to act for"),
                    ),
                    selected = forSelf,
                    onSelect = { forSelf = it },
                    label = "Who is this request for?",
                )
                MonarchTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = if (forSelf) "Full name" else "Their full name",
                    helperText = "Optional",
                )
                MonarchTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = if (forSelf) "Email" else "Their email",
                    helperText = "Used to identify the account and to reply about this request.",
                    errorText = if (submitted && !emailValid) "Enter an email address like name@example.com." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                if (!forSelf) {
                    MonarchTextField(
                        value = agentName,
                        onValueChange = { agentName = it },
                        label = "Your full name",
                        helperText = "You may be asked for proof that you can act for this person.",
                        errorText = if (submitted && agentName.isBlank()) "Enter your full name." else null,
                    )
                }
                MonarchCountrySelect(
                    selected = country,
                    onSelectedChange = { country = it },
                    errorText = if (submitted && country == null) "Choose a country or region." else null,
                    labels = MonarchCountrySelectLabels(label = "Country or region where the person lives"),
                )
                MonarchTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = "Details",
                    helperText = "Anything that helps find the information or explains the request.",
                    errorText = if (submitted && type == "correct" && details.isBlank()) {
                        "Describe what needs correcting."
                    } else {
                        null
                    },
                    singleLine = false,
                )
                MonarchCheckboxRow(
                    checked = accurate,
                    onCheckedChange = { accurate = it },
                    label = "The information I gave is accurate",
                )
                MonarchButton(
                    text = "Send request",
                    onClick = {
                        submitted = true
                        // Send the request to your API here; show its reference when it succeeds.
                        if (problems.isEmpty()) reference = "DSR-1042"
                    },
                    variant = MonarchButtonVariant.Primary,
                )
            }
        }
    }
}
