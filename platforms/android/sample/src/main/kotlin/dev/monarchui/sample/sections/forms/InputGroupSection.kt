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
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of InputGroup: MonarchTextField with attached text and a button. */
object InputGroupSection : CataloguePage {
    override val title = "InputGroup"

    @Composable
    override fun Content() {
        var address by rememberSaveable { mutableStateOf("") }
        var weight by rememberSaveable { mutableStateOf("12.5") }
        var copied by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A prefix and an attached button")
        MonarchTextField(
            value = address,
            onValueChange = {
                address = it
                copied = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = "Website",
            placeholder = "example.com",
            helperText = if (copied) "Copied." else null,
            leadingIcon = { MonarchText(text = "https://", color = MonarchTheme.colors.textMuted) },
            trailingIcon = {
                MonarchButton(
                    text = "Copy",
                    onClick = { copied = true },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        CatalogueCaption("A unit after the number")
        MonarchTextField(
            value = weight,
            onValueChange = { weight = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Weight",
            trailingIcon = { MonarchText(text = "kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}
