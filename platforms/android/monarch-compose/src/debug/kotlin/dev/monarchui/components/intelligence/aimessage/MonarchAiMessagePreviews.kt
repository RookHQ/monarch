@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aimessage

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFinishReason
import dev.monarchui.components.intelligence.aiconversation.MonarchAiReasoningPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewCreated = "2026-11-05T09:05:00.000Z"

@MonarchPreviews
@Composable
internal fun MonarchAiMessagePreview() {
    PreviewFrame {
        MonarchAiMessage(
            message = MonarchAiConversationMessage(
                id = "u1",
                role = MonarchAiRole.User,
                parts = listOf(MonarchAiTextPart("When is invoice 42 due?")),
                createdAt = PreviewCreated,
            ),
            timeZone = "UTC",
            onEdit = {},
        )
        MonarchAiMessage(
            message = MonarchAiConversationMessage(
                id = "a1",
                role = MonarchAiRole.Assistant,
                parts = listOf(
                    MonarchAiReasoningPart("Look up invoice 42.", durationMs = 2_000),
                    MonarchAiTextPart("Invoice 42 is due on **4 November**."),
                    MonarchAiSourcePart("s1", "Billing handbook", kind = MonarchAiSourceKind.Doc),
                ),
                createdAt = PreviewCreated,
                agent = MonarchAiAgentRef("Ledger", role = "Billing"),
                finishReason = MonarchAiFinishReason.Length,
            ),
            timeZone = "UTC",
            onRegenerate = {},
            onFeedback = {},
        )
    }
}
