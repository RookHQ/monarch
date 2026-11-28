package dev.monarchui.components.intelligence.aifeedback

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchAiFeedbackPreview() {
    PreviewFrame {
        MonarchAiFeedback(selection = null, onSelectionChange = {})
        MonarchAiFeedback(selection = MonarchAiMessageFeedback.Up, onSelectionChange = {})
        MonarchAiFeedback(
            selection = MonarchAiMessageFeedback.Down,
            onSelectionChange = {},
            asksDetails = MonarchAiFeedbackDetails.None,
            icon = { _, _ -> MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
    }
}
