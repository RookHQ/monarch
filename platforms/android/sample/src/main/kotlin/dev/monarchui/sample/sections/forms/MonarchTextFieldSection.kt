package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Input. */
object MonarchTextFieldSection : CataloguePage {
    override val title = "Input"

    @Composable
    override fun Content() {
        var email by rememberSaveable { mutableStateOf("") }
        MonarchTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Email",
            helperText = "Used to sign in.",
            errorText = if (email.isNotEmpty() && "@" !in email) "Enter a complete email address." else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        MonarchTextField(
            value = "Read only",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Workspace",
            readOnly = true,
        )
        MonarchTextField(
            value = "Disabled",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Plan",
            enabled = false,
        )
    }
}
