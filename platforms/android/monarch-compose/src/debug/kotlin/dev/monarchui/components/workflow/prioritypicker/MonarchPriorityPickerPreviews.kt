package dev.monarchui.components.workflow.prioritypicker

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPriorityPickerPreview() {
    PreviewFrame {
        MonarchPriorityLevel.entries.forEach { level ->
            MonarchPriorityPicker(value = level, onValueChange = {})
        }
        MonarchPriorityPicker(
            value = MonarchPriorityLevel.High,
            onValueChange = {},
            size = MonarchPriorityPickerSize.Sm,
        )
        MonarchPriorityPicker(value = MonarchPriorityLevel.Urgent, onValueChange = {}, showLabel = false)
        MonarchPriorityPicker(value = null, onValueChange = {}, enabled = false)
        Row { MonarchPriorityLevel.entries.forEach { MonarchPriorityGlyph(it) } }
    }
}
