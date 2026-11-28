package dev.monarchui.components.intelligence.aivoicemode

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiVoiceModePreview() {
    PreviewFrame {
        MonarchAiVoiceMode(
            state = MonarchAiVoiceModeState.Speaking,
            level = PreviewLevel,
            agent = MonarchAiAgentRef(name = "Ada"),
            captions = listOf(
                MonarchAiConversationMessage(
                    id = "1",
                    role = MonarchAiRole.User,
                    parts = listOf(MonarchAiTextPart("What is on my calendar today?")),
                    createdAt = "2025-03-10T12:00:00.000Z",
                ),
                MonarchAiConversationMessage(
                    id = "2",
                    role = MonarchAiRole.Assistant,
                    parts = listOf(MonarchAiTextPart("You have two meetings", MonarchAiPartState.Streaming)),
                    createdAt = "2025-03-10T12:00:02.000Z",
                ),
            ),
            elapsedSeconds = PreviewSeconds,
            size = MonarchAiVoiceModeSize.Compact,
            onMuteChange = {},
            onInterrupt = {},
            onEnd = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiVoiceModeErrorPreview() {
    PreviewFrame {
        MonarchAiVoiceMode(state = MonarchAiVoiceModeState.Error, size = MonarchAiVoiceModeSize.Compact, onRetry = {})
    }
}

private const val PreviewLevel = 0.6f
private const val PreviewSeconds = 65
