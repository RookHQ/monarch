package com.rookhq.monarch.components.intelligence.aitonepicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
