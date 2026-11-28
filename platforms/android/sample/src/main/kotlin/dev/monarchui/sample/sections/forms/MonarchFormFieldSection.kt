package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.formfield.MonarchFormField
import dev.monarchui.components.forms.formfield.MonarchFormFieldLayout
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Field. */
object MonarchFormFieldSection : CataloguePage {
    override val title = "Field"

    @Composable
    override fun Content() {
        var email by rememberSaveable { mutableStateOf("") }
        var company by rememberSaveable { mutableStateOf("") }
        var digest by rememberSaveable { mutableStateOf(true) }
        val invalid = email.isNotEmpty() && '@' !in email
        val emailError = if (invalid) "Enter an email address like name@example.com." else null
        CatalogueCaption("Required, with an error until it has an @")
        MonarchFormField(
            label = "Email",
            required = true,
            helperText = "We send receipts here.",
            errorText = emailError,
        ) { MonarchTextField(value = email, onValueChange = { email = it }, modifier = it) }
        CatalogueCaption("Optional")
        MonarchFormField(label = "Company", optionalText = "Optional") {
            MonarchTextField(value = company, onValueChange = { company = it }, modifier = it)
        }
        CatalogueCaption("Row layout with a switch")
        MonarchFormField(
            label = "Weekly digest",
            layout = MonarchFormFieldLayout.Row,
            helperText = "A summary of activity, sent on Monday mornings.",
        ) { Switch(checked = digest, onCheckedChange = { digest = it }, modifier = it) }
        CatalogueCaption("Disabled")
        MonarchFormField(label = "Plan", enabled = false, helperText = "Set by your admin.") {
            MonarchTextField(value = "Team", onValueChange = {}, modifier = it, enabled = false)
        }
    }
}
