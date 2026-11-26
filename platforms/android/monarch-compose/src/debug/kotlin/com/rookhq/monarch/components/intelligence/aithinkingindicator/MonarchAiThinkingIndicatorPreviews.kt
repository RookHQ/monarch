package com.rookhq.monarch.components.intelligence.aithinkingindicator

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiThinkingIndicatorPreview() {
    PreviewFrame {
        MonarchAiThinkingIndicator(announce = false)
        MonarchAiThinkingIndicator(
            variant = MonarchAiThinkingIndicatorVariant.Mark,
            label = "Searching your files…",
            startedAt = System.currentTimeMillis() - PreviewElapsedMillis,
            announce = false,
        )
    }
}

private const val PreviewElapsedMillis = 65_000L
