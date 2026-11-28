package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBar
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of FormActionsBar. */
object MonarchFormActionsBarSection : CataloguePage {
    override val title = "FormActionsBar"

    @Composable
    override fun Content() {
        var saved by rememberSaveable { mutableStateOf("Rook") }
        var name by rememberSaveable { mutableStateOf("Rook") }
        var pending by rememberSaveable { mutableStateOf(false) }
        var failing by rememberSaveable { mutableStateOf(false) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Edit the field")
        MonarchTextField(value = name, onValueChange = { name = it }, label = "Workspace name")
        MonarchSwitchRow(checked = pending, onCheckedChange = { pending = it }, label = "Save in progress")
        MonarchSwitchRow(checked = failing, onCheckedChange = { failing = it }, label = "Make the next save fail")
        MonarchFormActionsBar(
            dirty = name != saved,
            onSave = {
                if (failing) {
                    error = "Your changes could not be saved. Try again."
                } else {
                    saved = name
                    error = null
                }
            },
            onDiscard = {
                name = saved
                error = null
            },
            pending = pending,
            error = error,
            windowInsets = WindowInsets(0),
        )
    }
}
