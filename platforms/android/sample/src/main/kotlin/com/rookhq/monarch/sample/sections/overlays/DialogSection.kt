package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Dialog: MonarchDialog with a message, and with a form as its content. */
object DialogSection : CataloguePage {
    override val title = "Dialog"

    @Composable
    override fun Content() {
        var message by rememberSaveable { mutableStateOf(false) }
        var form by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("Q3 report") }
        CatalogueCaption("A message")
        MonarchButton(text = "Export", onClick = { message = true })
        CatalogueCaption("A form as the content")
        MonarchButton(text = "Rename report", onClick = { form = true })
        MonarchText(text = "Report: $name", style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        if (message) {
            MonarchDialog(
                onDismissRequest = { message = false },
                title = "Export started",
                description = "We will email you a link when the file is ready.",
                actions = {
                    MonarchButton(text = "OK", onClick = { message = false }, variant = MonarchButtonVariant.Primary)
                },
            )
        }
        if (form) {
            MonarchDialog(
                onDismissRequest = { form = false },
                title = "Rename report",
                actions = {
                    MonarchButton(text = "Cancel", onClick = { form = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(text = "Save", onClick = { form = false }, variant = MonarchButtonVariant.Primary)
                },
            ) {
                MonarchTextField(value = name, onValueChange = { name = it }, label = "Name")
            }
        }
    }
}
