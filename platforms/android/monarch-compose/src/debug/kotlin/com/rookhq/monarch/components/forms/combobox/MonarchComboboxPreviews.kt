package com.rookhq.monarch.components.forms.combobox

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewPeople = listOf(
    MonarchComboboxOption("ada", "Ada Lovelace", hint = "ada@example.com", group = "Team"),
    MonarchComboboxOption("alan", "Alan Turing", description = "On leave", enabled = false, group = "Team"),
    MonarchComboboxOption("grace", "Grace Hopper", group = "Guests"),
)

@MonarchPreviews
@Composable
internal fun MonarchComboboxPreview() {
    PreviewFrame {
        MonarchCombobox(label = "Assignee", options = previewPeople, selected = "ada", onSelectedChange = {})
        MonarchCombobox(
            label = "Assignee",
            options = previewPeople,
            selected = null,
            onSelectedChange = {},
            presentation = MonarchComboboxPresentation.Sheet,
            helperText = "Who works on it next.",
        )
        MonarchCombobox(
            label = "Assignee",
            options = previewPeople,
            selected = null,
            onSelectedChange = {},
            errorText = "Choose an assignee.",
        )
        MonarchCombobox("Assignee", options = previewPeople, selected = "grace", onSelectedChange = {}, enabled = false)
    }
}
