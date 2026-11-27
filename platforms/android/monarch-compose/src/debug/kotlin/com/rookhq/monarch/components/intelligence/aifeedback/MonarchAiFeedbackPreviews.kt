package com.rookhq.monarch.components.intelligence.aifeedback

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
