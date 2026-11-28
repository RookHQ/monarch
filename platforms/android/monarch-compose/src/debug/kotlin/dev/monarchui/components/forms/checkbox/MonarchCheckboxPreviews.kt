package dev.monarchui.components.forms.checkbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.state.ToggleableState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCheckboxPreview() {
    PreviewFrame {
        MonarchCheckbox(
            checked = true,
            onCheckedChange = {},
            label = "Email me updates",
            description = "About once a month.",
        )
        MonarchCheckbox(checked = false, onCheckedChange = {}, label = "Product news")
        MonarchCheckbox(state = ToggleableState.Indeterminate, onClick = {}, label = "Select all")
        MonarchCheckbox(checked = false, onCheckedChange = {}, label = "Unavailable", enabled = false)
    }
}
