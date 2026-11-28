package dev.monarchui.components.intelligence.aitypingdots

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiTypingDotsPreview() {
    PreviewFrame {
        MonarchAiTypingDots(size = MonarchAiTypingDotsSize.Sm, announce = false)
        MonarchAiTypingDots(announce = false)
    }
}
