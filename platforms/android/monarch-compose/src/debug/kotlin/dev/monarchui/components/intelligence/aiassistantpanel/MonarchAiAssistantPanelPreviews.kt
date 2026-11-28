@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiassistantpanel

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aistarterprompts.MonarchAiStarterPrompt
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens

@MonarchPreviews
@Composable
internal fun MonarchAiAssistantPanelPreview() {
    PreviewFrame {
        MonarchAiAssistantPanel(
            title = "Assistant",
            messages = listOf(
                MonarchAiConversationMessage(
                    id = "u1",
                    role = MonarchAiRole.User,
                    parts = listOf(MonarchAiTextPart("Summarize this page")),
                    createdAt = "2026-11-05T09:05:00.000Z",
                ),
                MonarchAiConversationMessage(
                    id = "a1",
                    role = MonarchAiRole.Assistant,
                    parts = listOf(MonarchAiTextPart("The quarterly plan has **three goals** and two open risks.")),
                    createdAt = "2026-11-05T09:05:02.000Z",
                ),
            ),
            open = true,
            onOpenChange = {},
            onSend = {},
            modifier = Modifier.height(MonarchTokens.Breakpoint.sm.dp),
            placement = MonarchAiAssistantPanelPlacement.Inline,
            agent = MonarchAiAgentRef(name = "Dossier", role = "Workspace assistant"),
            context = listOf(MonarchAiContextItem("page", MonarchAiContextKind.Page, "Quarterly plan")),
            onMinimizedChange = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiAssistantPanelEmptyPreview() {
    PreviewFrame {
        MonarchAiAssistantPanel(
            title = "Assistant",
            messages = emptyList(),
            open = true,
            onOpenChange = {},
            onSend = {},
            modifier = Modifier.height(MonarchTokens.Breakpoint.sm.dp),
            placement = MonarchAiAssistantPanelPlacement.Inline,
            starters = listOf(
                MonarchAiStarterPrompt("sum", "Summarize this page", "Summarize this page."),
                MonarchAiStarterPrompt("week", "What changed this week?", "What changed this week?"),
            ),
        )
    }
}
