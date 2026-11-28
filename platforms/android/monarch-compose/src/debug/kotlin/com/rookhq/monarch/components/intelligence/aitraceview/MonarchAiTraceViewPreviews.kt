@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchAiTraceViewPreview() {
    val spans = listOf(
        MonarchAiTraceSpan("run", MonarchAiTraceSpanKind.Agent, "Answer", MonarchAiTraceSpanStatus.Ok, 0, 4_200),
        MonarchAiTraceSpan(
            "plan",
            MonarchAiTraceSpanKind.Model,
            "Plan",
            MonarchAiTraceSpanStatus.Ok,
            100,
            1_300,
            parentId = "run",
            inputTokens = 1_200,
            outputTokens = 180,
            costMicros = 4_200,
        ),
        MonarchAiTraceSpan(
            "search",
            MonarchAiTraceSpanKind.Tool,
            "search_documents",
            MonarchAiTraceSpanStatus.Error,
            1_400,
            2_000,
            parentId = "run",
            error = "Timed out",
        ),
    )
    PreviewFrame {
        MonarchAiTraceView(spans = spans, expandIcon = { Text("›") }, locale = Locale.US)
        MonarchAiTraceView(spans = emptyList(), expandIcon = { Text("›") })
    }
}
