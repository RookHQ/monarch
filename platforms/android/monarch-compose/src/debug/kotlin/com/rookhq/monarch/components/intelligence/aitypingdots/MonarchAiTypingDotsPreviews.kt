package com.rookhq.monarch.components.intelligence.aitypingdots

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiTypingDotsPreview() {
    PreviewFrame {
        MonarchAiTypingDots(size = MonarchAiTypingDotsSize.Sm, announce = false)
        MonarchAiTypingDots(announce = false)
    }
}
