package com.rookhq.monarch.components.intelligence.aireasoning

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
