package dev.monarchui.components.datagrid.editablecell

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchEditableCellPreview() {
    PreviewFrame {
        MonarchEditableCell(value = "Acme", label = "Company", editor = MonarchEditableCellEditor.Text, onCommit = {})
        MonarchEditableCell(
            value = 1250.5,
            label = "Amount for Acme",
            editor = MonarchEditableCellEditor.Number(min = 0.0),
            onCommit = {},
        )
        MonarchEditableCell(
            value = "open",
            label = "Status for Acme",
            editor = MonarchEditableCellEditor.Select(
                listOf(MonarchSelectOption("open", "Open"), MonarchSelectOption("won", "Won")),
            ),
            onCommit = {},
        )
        MonarchEditableCell(
            value = 1_767_225_600_000L,
            label = "Close date for Acme",
            editor = MonarchEditableCellEditor.Date(),
            onCommit = {},
        )
        MonarchEditableCell<String>(
            value = null,
            label = "Notes",
            editor = MonarchEditableCellEditor.Text,
            onCommit = {},
            placeholder = "Add a note",
        )
        MonarchEditableCell(
            value = "Ada",
            label = "Owner",
            editor = MonarchEditableCellEditor.Text,
            onCommit = {},
            startEditing = true,
        )
        MonarchEditableCell(
            value = "Locked",
            label = "Region",
            editor = MonarchEditableCellEditor.Text,
            onCommit = {},
            enabled = false,
        )
    }
}
