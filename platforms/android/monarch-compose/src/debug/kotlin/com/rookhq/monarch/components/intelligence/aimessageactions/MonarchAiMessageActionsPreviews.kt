package com.rookhq.monarch.components.intelligence.aimessageactions

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchAiMessageActionsPreview() {
    PreviewFrame {
        MonarchAiMessageActions(
            messageText = "The invoice is due on 4 November.",
            onCopy = {},
            onRegenerate = {},
            feedback = MonarchAiMessageFeedback.Up,
            onFeedback = {},
            onShare = {},
            onReadAloud = {},
            icon = { _, _ -> MonarchIcon(PreviewIcon, contentDescription = null) },
        )
        MonarchAiMessageActions(
            messageText = "The invoice is due on 4 November.",
            onCopy = {},
            onRegenerate = {},
            feedback = MonarchAiMessageFeedback.Down,
            onFeedback = {},
        )
        MonarchAiMessageActions(messageText = "Draft", onCopy = {}, onEdit = {}, enabled = false)
    }
}
