package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.autosaveindicator.MonarchAutosaveIndicator
import dev.monarchui.components.forms.autosaveindicator.rememberMonarchAutosaveState
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CataloguePage
import java.io.IOException
import kotlinx.coroutines.delay

/** The catalogue page of AutosaveIndicator: a note saved a second after typing stops, with a simulated failure. */
object MonarchAutosaveIndicatorSection : CataloguePage {
    override val title = "AutosaveIndicator"

    private const val SAVE_MILLIS = 600L

    @Composable
    override fun Content() {
        var note by rememberSaveable { mutableStateOf("Call the supplier on Monday") }
        var offline by rememberSaveable { mutableStateOf(false) }
        var saves by rememberSaveable { mutableIntStateOf(0) }
        val autosave = rememberMonarchAutosaveState(
            value = note,
            onSave = {
                delay(SAVE_MILLIS)
                if (offline) throw IOException("You are offline")
                saves++
            },
            retries = 0,
        )
        MonarchTextField(value = note, onValueChange = { note = it }, label = "Note", singleLine = false)
        MonarchAutosaveIndicator(state = autosave, errorMessage = { "Couldn't save: ${it.message}" })
        MonarchCheckboxRow(
            checked = offline,
            onCheckedChange = { offline = it },
            label = "Simulate being offline",
            description = "Saves so far: $saves",
        )
    }
}
