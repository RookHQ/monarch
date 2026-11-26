package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.components.intelligence.aitoolcall.MonarchAiToolCall
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

private const val SampleCount = 3.0
private const val InvoiceCount = 40
private const val FirstInvoice = 1000
private const val SampleMaxLength = 200

private val Steps = listOf(
    MonarchAiToolCallState.PendingApproval,
    MonarchAiToolCallState.Running,
    MonarchAiToolCallState.Succeeded,
)

/** The catalogue page of AiToolCall. */
object MonarchAiToolCallSection : CataloguePage {
    override val title = "AiToolCall"

    @Composable
    override fun Content() {
        var step by rememberSaveable { mutableIntStateOf(0) }
        val state = Steps[step]
        CatalogueCaption("A call moving through its states; each change is announced")
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(
                id = "call-1",
                name = "search_documents",
                input = MonarchAiJSON.Object(mapOf("query" to MonarchAiJSON.Text("Q3 plan"))),
                state = state,
                output = if (state == MonarchAiToolCallState.Succeeded) {
                    MonarchAiJSON.Object(mapOf("count" to MonarchAiJSON.Number(SampleCount)))
                } else {
                    null
                },
                startedAt = "2026-03-02T09:00:00Z",
                endedAt = if (state == MonarchAiToolCallState.Succeeded) "2026-03-02T09:00:01.400Z" else null,
            ),
            icon = { MonarchIcon(SampleIcons.Wrench, contentDescription = null) },
            describeTool = { part -> part.input["query"]?.stringValue?.let { "Searched documents for “$it”" } },
        )
        MonarchButton(text = "Next state", onClick = { step = (step + 1) % Steps.size })
        CatalogueCaption("A failed call shows its error without opening")
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(
                id = "call-2",
                name = "send_email",
                input = MonarchAiJSON.Object(mapOf("to" to MonarchAiJSON.Text("ada@example.com"))),
                state = MonarchAiToolCallState.Failed,
                error = "The mail server refused the message.",
            ),
            toolLabels = mapOf("send_email" to "Send an email"),
        )
        CatalogueCaption("A long result is cut with Show all")
        MonarchAiToolCall(
            part = MonarchAiToolCallPart(
                id = "call-3",
                name = "list_invoices",
                state = MonarchAiToolCallState.Succeeded,
                output = MonarchAiJSON.Array(
                    List(InvoiceCount) {
                        MonarchAiJSON.Object(mapOf("number" to MonarchAiJSON.Text("INV-${FirstInvoice + it}")))
                    },
                ),
            ),
            maxValueLength = SampleMaxLength,
        )
    }
}
