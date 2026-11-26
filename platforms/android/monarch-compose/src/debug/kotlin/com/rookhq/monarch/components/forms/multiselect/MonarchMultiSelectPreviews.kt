package com.rookhq.monarch.components.forms.multiselect

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
