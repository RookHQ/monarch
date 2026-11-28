package com.rookhq.monarch.components.forms.colorpicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchColorPickerPreview() {
    PreviewFrame {
        MonarchColorPicker(value = "#2563eb", onValueChange = {}, label = "Label color")
        MonarchColorPicker(value = null, onValueChange = {}, label = "Disabled", enabled = false, allowCustom = false)
    }
}
