package dev.monarchui.components.intelligence.aitoolcall

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewInput = MonarchAiJSON.Object(mapOf("query" to MonarchAiJSON.Text("Q3 plan")))

@MonarchPreviews
@Composable
internal fun MonarchAiToolCallPreview() {
    PreviewFrame {
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(
                id = "1",
                name = "search_documents",
                input = PreviewInput,
                state = MonarchAiToolCallState.Succeeded,
                output = MonarchAiJSON.Object(mapOf("count" to MonarchAiJSON.Number(3.0))),
                startedAt = "2026-03-02T09:00:00Z",
                endedAt = "2026-03-02T09:00:01Z",
            ),
            expanded = true,
        )
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(
                id = "2",
                name = "send_email",
                state = MonarchAiToolCallState.Failed,
                error = "The mail server refused the message.",
            ),
        )
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(id = "3", name = "run_query", state = MonarchAiToolCallState.PendingApproval),
        )
    }
}
