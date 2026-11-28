package dev.monarchui.components.intelligence.aireasoning

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiReasoningPart
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private const val PreviewText = "The user wants last quarter's churn. The table has monthly rows, so I sum Q3 first."

@MonarchPreviews
@Composable
internal fun MonarchAiReasoningPreview() {
    PreviewFrame {
        MonarchAiReasoning(
            part = MonarchAiReasoningPart(text = PreviewText, durationMs = 12_000L),
            initiallyExpanded = true,
            expandIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchAiReasoning(part = MonarchAiReasoningPart(text = PreviewText, state = MonarchAiPartState.Streaming))
        MonarchAiReasoning(
            part = MonarchAiReasoningPart(text = "", redacted = true),
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
    }
}
