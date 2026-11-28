@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aithread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationStatus
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private const val PreviewHeightSteps = 4

/** 5 November 2026, 09:05 UTC. */
private const val PreviewNow = 1_793_869_500_000L

private val PreviewMessages = listOf(
    MonarchAiConversationMessage(
        id = "u1",
        role = MonarchAiRole.User,
        parts = listOf(MonarchAiTextPart("When is invoice 42 due?")),
        createdAt = "2026-11-05T09:04:00.000Z",
    ),
    MonarchAiConversationMessage(
        id = "a1",
        role = MonarchAiRole.Assistant,
        parts = listOf(MonarchAiTextPart("Invoice 42 is due on **4 December**.")),
        createdAt = "2026-11-05T09:04:05.000Z",
    ),
    MonarchAiConversationMessage(
        id = "u2",
        role = MonarchAiRole.User,
        parts = listOf(MonarchAiTextPart("Send a reminder a week before.")),
        createdAt = "2026-11-05T09:05:00.000Z",
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchAiThreadPreview() {
    PreviewFrame {
        MonarchAiThread(
            messages = PreviewMessages,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            status = MonarchAiConversationStatus.Submitted,
            hasEarlier = true,
            timeZone = "UTC",
            nowMillis = PreviewNow,
            onLoadEarlier = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiThreadNoticePreview() {
    PreviewFrame {
        MonarchAiThread(
            messages = PreviewMessages.take(1),
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            offline = true,
            queuedCount = 1,
            timeZone = "UTC",
            nowMillis = PreviewNow,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiThreadEmptyPreview() {
    PreviewFrame {
        MonarchAiThread(
            messages = emptyList(),
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            empty = { MonarchText("Ask anything about your invoices.") },
        )
    }
}
