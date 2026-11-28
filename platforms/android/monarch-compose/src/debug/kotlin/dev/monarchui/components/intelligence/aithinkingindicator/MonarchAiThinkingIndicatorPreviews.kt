package dev.monarchui.components.intelligence.aithinkingindicator

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
