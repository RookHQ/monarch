package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.radiorow.MonarchRadioRow
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of ChoiceDialog: MonarchDialog with MonarchRadioRow options. */
object ChoiceDialogSection : CataloguePage {
    override val title = "ChoiceDialog"

    private val formats = listOf(
        "PDF" to "For printing and sharing",
        "CSV" to "For spreadsheets",
        "JSON" to "For developers",
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var chosen by rememberSaveable { mutableStateOf<String?>(null) }
        var exported by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Pick one of a few options")
        MonarchButton(text = "Export", onClick = { open = true })
        exported?.let { MonarchText(text = "Exported as $it") }
        if (open) {
            MonarchDialog(
                onDismissRequest = { open = false },
                title = "Export as",
                content = {
                    Column(modifier = Modifier.selectableGroup()) {
                        formats.forEach { (name, description) ->
                            MonarchRadioRow(
                                selected = chosen == name,
                                onClick = { chosen = name },
                                label = name,
                                description = description,
                            )
                        }
                    }
                },
                actions = {
                    MonarchButton(text = "Cancel", onClick = { open = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(
                        text = "Export",
                        onClick = {
                            exported = chosen
                            open = false
                        },
                        variant = MonarchButtonVariant.Primary,
                        enabled = chosen != null,
                    )
                },
            )
        }
    }
}
