package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.intelligence.aiagentruntimeline.MonarchAiAgentRunTimeline
import com.rookhq.monarch.components.intelligence.aiagentruntimeline.MonarchAiRunEvent
import com.rookhq.monarch.components.intelligence.aiagentruntimeline.MonarchAiRunEventKind
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val T0 = 1_700_000_000_000L

private val Events = listOf(
    MonarchAiRunEvent(
        id = "1",
        kind = MonarchAiRunEventKind.Model,
        name = "plan",
        startedAt = T0,
        endedAt = T0 + 900,
        inputTokens = 1_200,
        outputTokens = 340,
        costMicros = 4_200,
        messageId = "m1",
    ),
    MonarchAiRunEvent(
        id = "2",
        kind = MonarchAiRunEventKind.Tool,
        name = "search_invoices",
        startedAt = T0 + 1_000,
        endedAt = T0 + 2_600,
        detail = "{\"status\":\"unpaid\",\"limit\":20}",
        partId = "p2",
    ),
    MonarchAiRunEvent("3", MonarchAiRunEventKind.Handoff, "billing_agent", T0 + 2_700, T0 + 2_800),
    MonarchAiRunEvent("4", MonarchAiRunEventKind.Error, "send_email", T0 + 2_900, T0 + 3_100, detail = "SMTP 550"),
    MonarchAiRunEvent("5", MonarchAiRunEventKind.Model, "answer", T0 + 3_200, inputTokens = 2_400),
)

/** The catalogue page of AiAgentRunTimeline. */
object MonarchAiAgentRunTimelineSection : CataloguePage {
    override val title = "AiAgentRunTimeline"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("A run with a running answer; open an event for its details")
        MonarchAiAgentRunTimeline(events = Events, now = T0 + 4_000, onOpenPart = { shown = it.name })
        shown?.let { MonarchText(text = "Would show $it in the conversation") }
    }
}
