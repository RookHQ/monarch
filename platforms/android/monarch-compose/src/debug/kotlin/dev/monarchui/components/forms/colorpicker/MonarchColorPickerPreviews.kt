package dev.monarchui.components.forms.colorpicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchColorPickerPreview() {
    PreviewFrame {
        MonarchColorPicker(value = "#2563eb", onValueChange = {}, label = "Label color")
        MonarchColorPicker(value = null, onValueChange = {}, label = "Disabled", enabled = false, allowCustom = false)
    }
}
