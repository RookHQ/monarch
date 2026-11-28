package dev.monarchui.components.intelligence.aiagentruntimeline

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val T0 = 1_700_000_000_000L

@MonarchPreviews
@Composable
internal fun MonarchAiAgentRunTimelinePreview() {
    val events = listOf(
        MonarchAiRunEvent("1", MonarchAiRunEventKind.Model, "plan", T0, T0 + 900, inputTokens = 1_200),
        MonarchAiRunEvent("2", MonarchAiRunEventKind.Tool, "search_invoices", T0 + 1_000, T0 + 2_600),
        MonarchAiRunEvent("3", MonarchAiRunEventKind.Error, "send_email", T0 + 2_700, T0 + 2_900),
        MonarchAiRunEvent("4", MonarchAiRunEventKind.Model, "answer", T0 + 3_000),
    )
    PreviewFrame {
        MonarchAiAgentRunTimeline(events = events, now = T0 + 4_200)
        MonarchAiAgentRunTimeline(events = events, now = T0 + 4_200, initialMode = MonarchAiAgentRunTimelineMode.Table)
    }
}
