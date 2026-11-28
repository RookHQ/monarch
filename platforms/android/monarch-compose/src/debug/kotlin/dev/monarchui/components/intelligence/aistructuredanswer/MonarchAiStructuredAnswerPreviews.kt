package dev.monarchui.components.intelligence.aistructuredanswer

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewSchema = listOf(
    MonarchAiStructuredField("candidate", "Candidate"),
    MonarchAiStructuredField("stage", "Stage", MonarchAiStructuredFieldType.Badge, tone = { MonarchTone.Info }),
    MonarchAiStructuredField("score", "Overall score", MonarchAiStructuredFieldType.Number),
    MonarchAiStructuredField("strengths", "Strengths", MonarchAiStructuredFieldType.List),
    MonarchAiStructuredField("summary", "Summary", MonarchAiStructuredFieldType.Markdown),
)

private val PreviewData = mapOf(
    "candidate" to MonarchAiJSON.Text("Alex Morgan"),
    "stage" to MonarchAiJSON.Text("Final interview"),
    "score" to MonarchAiJSON.Number(4.2),
    "strengths" to MonarchAiJSON.Array(listOf(MonarchAiJSON.Text("System design"), MonarchAiJSON.Text("Writing"))),
    "summary" to MonarchAiJSON.Text("Interviewers agree on **technical depth**."),
)

@MonarchPreviews
@Composable
internal fun MonarchAiStructuredAnswerPreview() {
    PreviewFrame {
        MonarchAiStructuredAnswer(schema = PreviewSchema, data = PreviewData, title = "Interview brief")
        MonarchAiStructuredAnswer(
            schema = PreviewSchema,
            data = PreviewData.filterKeys { it == "candidate" },
            layout = MonarchAiStructuredAnswerLayout.Grid,
            streaming = true,
        )
    }
}
