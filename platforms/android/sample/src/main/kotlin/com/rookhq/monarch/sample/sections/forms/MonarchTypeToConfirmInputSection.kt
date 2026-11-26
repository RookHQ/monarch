package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import com.rookhq.monarch.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of TypeToConfirmInput. */
object MonarchTypeToConfirmInputSection : CataloguePage {
    override val title = "TypeToConfirmInput"

    @Composable
    override fun Content() {
        var typed by rememberSaveable { mutableStateOf("") }
        var tried by rememberSaveable { mutableStateOf(false) }
        var deleted by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Guarding a delete button")
        MonarchTypeToConfirmInput(
            phrase = "acme-site",
            value = typed,
            onValueChange = { typed = it },
            helperText = if (deleted) "Deleted." else "This deletes the site and its 14 pages.",
            showMismatch = tried,
        )
        MonarchButton(
            text = "Delete site",
            onClick = {
                tried = true
                if (MonarchTypeToConfirmInputDefaults.matches(typed, "acme-site")) deleted = true
            },
            variant = MonarchButtonVariant.Danger,
        )
        CatalogueCaption("Disabled")
        MonarchTypeToConfirmInput(phrase = "acme-site", value = "", onValueChange = {}, enabled = false)
    }
}
