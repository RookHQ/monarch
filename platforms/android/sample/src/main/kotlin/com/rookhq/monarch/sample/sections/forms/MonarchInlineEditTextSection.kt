package com.rookhq.monarch.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.inlineedittext.MonarchInlineEditText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/** The catalogue page of InlineEditText. */
object MonarchInlineEditTextSection : CataloguePage {
    override val title = "InlineEditText"

    private const val SAVE_DELAY_MILLIS = 600L

    @Composable
    override fun Content() {
        var title by rememberSaveable { mutableStateOf("Quarterly plan") }
        var name by rememberSaveable { mutableStateOf("Ada") }
        var notes by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A title saved after a short wait")
        MonarchInlineEditText(
            value = title,
            onSave = {
                delay(SAVE_DELAY_MILLIS)
                title = it
            },
            label = "Title",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Untitled",
            editIcon = { Icon(imageVector = SampleIcons.Pencil, contentDescription = null) },
        )
        CatalogueCaption("Validation, and a save that fails for \"error\"")
        MonarchInlineEditText(
            value = name,
            onSave = {
                delay(SAVE_DELAY_MILLIS)
                check(it != "error") { "The server refused the name." }
                name = it
            },
            label = "Name",
            modifier = Modifier.fillMaxWidth(),
            validate = { if (it.isEmpty()) "Enter a name." else null },
        )
        CatalogueCaption("Several lines")
        MonarchInlineEditText(
            value = notes,
            onSave = { notes = it },
            label = "Notes",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Add notes",
            multiline = true,
        )
        CatalogueCaption("Disabled")
        MonarchInlineEditText(value = "Read only", onSave = {}, label = "Owner", enabled = false)
    }
}
