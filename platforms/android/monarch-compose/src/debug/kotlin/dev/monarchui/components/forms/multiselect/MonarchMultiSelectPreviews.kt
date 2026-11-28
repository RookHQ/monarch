package dev.monarchui.components.forms.multiselect

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val teams = listOf(
    MonarchComboboxOption("design", "Design"),
    MonarchComboboxOption("eng", "Engineering"),
    MonarchComboboxOption("sales", "Sales"),
)

@MonarchPreviews
@Composable
internal fun MonarchMultiSelectPreview() {
    PreviewFrame {
        MonarchMultiSelect(label = "Teams", options = teams, selected = emptyList(), onSelectedChange = {})
        MonarchMultiSelect(
            label = "Teams",
            options = teams,
            selected = listOf("design", "sales"),
            onSelectedChange = {},
            maxSelections = 2,
            removeIcon = {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
            },
        )
        MonarchMultiSelect(
            label = "Teams",
            options = teams,
            selected = emptyList(),
            onSelectedChange = {},
            required = true,
            errorText = "Choose at least one team.",
        )
        MonarchMultiSelect(
            label = "Teams",
            options = teams,
            selected = listOf("eng"),
            onSelectedChange = {},
            enabled = false,
        )
    }
}
