@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aitraceview.MonarchAiTraceSpan
import dev.monarchui.components.intelligence.aitraceview.MonarchAiTraceSpanKind
import dev.monarchui.components.intelligence.aitraceview.MonarchAiTraceSpanStatus
import dev.monarchui.components.intelligence.aitraceview.MonarchAiTraceView
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** A chevron that points to the end; it mirrors in right-to-left layouts. */
private val TraceChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "Chevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8.6f, 7.4f)
            lineTo(10f, 6f)
            lineTo(16f, 12f)
            lineTo(10f, 18f)
            lineTo(8.6f, 16.6f)
            lineTo(13.2f, 12f)
            close()
        }
    }.build()
}

private val SampleSpans = listOf(
    MonarchAiTraceSpan("run", MonarchAiTraceSpanKind.Agent, "Answer question", MonarchAiTraceSpanStatus.Ok, 0, 4_200),
    MonarchAiTraceSpan(
        id = "plan",
        kind = MonarchAiTraceSpanKind.Model,
        name = "Plan",
        status = MonarchAiTraceSpanStatus.Ok,
        start = 100,
        end = 1_300,
        parentId = "run",
        inputTokens = 1_200,
        outputTokens = 180,
        costMicros = 4_200,
        model = "large-1",
        messageId = "m1",
    ),
    MonarchAiTraceSpan(
        id = "search",
        kind = MonarchAiTraceSpanKind.Tool,
        name = "search_documents",
        status = MonarchAiTraceSpanStatus.Error,
        start = 1_400,
        end = 2_000,
        parentId = "run",
        error = "Timed out after 600 ms",
        attributes = mapOf("index" to "contracts", "limit" to "20"),
        input = "{\"query\":\"renewal terms\"}",
    ),
    MonarchAiTraceSpan(
        id = "retry",
        kind = MonarchAiTraceSpanKind.Retrieval,
        name = "fetch_cached",
        status = MonarchAiTraceSpanStatus.Ok,
        start = 2_100,
        end = 2_350,
        parentId = "run",
        output = "3 passages",
    ),
    MonarchAiTraceSpan(
        id = "check",
        kind = MonarchAiTraceSpanKind.Guardrail,
        name = "Safety check",
        status = MonarchAiTraceSpanStatus.Ok,
        start = 2_400,
        end = 2_500,
        parentId = "run",
    ),
    MonarchAiTraceSpan(
        id = "answer",
        kind = MonarchAiTraceSpanKind.Model,
        name = "Write answer",
        status = MonarchAiTraceSpanStatus.Running,
        start = 2_600,
        parentId = "run",
    ),
)

/** The catalogue page of AiTraceView. */
object MonarchAiTraceViewSection : CataloguePage {
    override val title = "AiTraceView"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("A run with an error and a running answer; open a span for its details")
        MonarchAiTraceView(
            spans = SampleSpans,
            expandIcon = { MonarchIcon(TraceChevron, contentDescription = null, size = MonarchIconSize.Sm) },
            now = 4_200,
            onOpenPart = { shown = it.name },
        )
        shown?.let { MonarchText(text = "Would show $it in the conversation") }
    }
}
