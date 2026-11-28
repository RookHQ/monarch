package dev.monarchui.components.forms.checkboxrow

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCheckboxRowPreview() {
    PreviewFrame {
        MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Send me product updates")
        MonarchCheckboxRow(
            checked = false,
            onCheckedChange = {},
            label = "Share usage data",
            description = "Helps improve the product.",
        )
        MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Required by policy", enabled = false)
    }
}
