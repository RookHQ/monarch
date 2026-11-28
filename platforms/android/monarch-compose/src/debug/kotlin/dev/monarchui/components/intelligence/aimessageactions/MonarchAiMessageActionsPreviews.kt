package dev.monarchui.components.intelligence.aimessageactions

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
