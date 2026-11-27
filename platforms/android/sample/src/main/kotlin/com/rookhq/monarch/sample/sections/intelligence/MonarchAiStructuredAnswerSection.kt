package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswer
import com.rookhq.monarch.components.intelligence.aistructuredanswer.MonarchAiStructuredAnswerLayout
import com.rookhq.monarch.components.intelligence.aistructuredanswer.MonarchAiStructuredField
import com.rookhq.monarch.components.intelligence.aistructuredanswer.MonarchAiStructuredFieldType
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val BriefSchema = listOf(
    MonarchAiStructuredField("candidate", "Candidate"),
    MonarchAiStructuredField("role", "Role"),
    MonarchAiStructuredField("stage", "Stage", MonarchAiStructuredFieldType.Badge, tone = { MonarchTone.Info }),
    MonarchAiStructuredField("score", "Overall score", MonarchAiStructuredFieldType.Number),
    MonarchAiStructuredField("strengths", "Strengths", MonarchAiStructuredFieldType.List),
    MonarchAiStructuredField("risks", "Open questions", MonarchAiStructuredFieldType.List),
    MonarchAiStructuredField("summary", "Summary", MonarchAiStructuredFieldType.Markdown),
)

private val Brief = mapOf(
    "candidate" to MonarchAiJSON.Text("Alex Morgan"),
    "role" to MonarchAiJSON.Text("Senior engineer"),
    "stage" to MonarchAiJSON.Text("Final interview"),
    "score" to MonarchAiJSON.Number(4.2),
    "strengths" to MonarchAiJSON.Array(
        listOf(MonarchAiJSON.Text("Clear system design reasoning"), MonarchAiJSON.Text("Strong written communication")),
    ),
    "risks" to MonarchAiJSON.Array(listOf(MonarchAiJSON.Text("Limited on-call experience"))),
    "summary" to MonarchAiJSON.Text(
        "Interviewers agree on **technical depth**. Confirm availability for the start date.",
    ),
)

private val GridSchema = BriefSchema.take(4)

/** The catalogue page of AiStructuredAnswer. */
object MonarchAiStructuredAnswerSection : CataloguePage {
    override val title = "AiStructuredAnswer"

    @Composable
    override fun Content() {
        var arrived by rememberSaveable { mutableIntStateOf(BriefSchema.size) }
        val keys = BriefSchema.take(arrived).map { it.key }.toSet()
        CatalogueCaption("Streaming: press Next field to add one value at a time")
        MonarchAiStructuredAnswer(
            schema = BriefSchema,
            data = Brief.filterKeys { it in keys },
            title = "Interview brief, second round",
            streaming = arrived < BriefSchema.size,
        ) {
            MonarchButton(
                text = if (arrived < BriefSchema.size) "Next field" else "Replay",
                onClick = { arrived = if (arrived < BriefSchema.size) arrived + 1 else 0 },
                size = MonarchButtonSize.Sm,
            )
        }
        CatalogueCaption("Grid layout")
        MonarchAiStructuredAnswer(
            schema = GridSchema,
            data = Brief,
            layout = MonarchAiStructuredAnswerLayout.Grid,
        )
    }
}
