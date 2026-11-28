package dev.monarchui.components.intelligence.aitonepicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiTonePickerPreview() {
    PreviewFrame {
        MonarchAiTonePicker(selection = "technical", onSelectionChange = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiTonePickerCompactPreview() {
    PreviewFrame {
        MonarchAiTonePicker(
            selection = MonarchAiTone.CUSTOM_ID,
            onSelectionChange = {},
            variant = MonarchAiTonePickerVariant.Compact,
            customText = "Warm and brief",
            onCustomTextChange = {},
        )
    }
}
