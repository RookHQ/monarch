@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApproval
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SampleInput = MonarchAiJSON.Object(
    mapOf(
        "to" to MonarchAiJSON.Array(
            listOf(MonarchAiJSON.Text("ada@example.com"), MonarchAiJSON.Text("ben@example.com")),
        ),
        "subject" to MonarchAiJSON.Text("Q3 plan"),
    ),
)

private const val SampleRecordCount = 12.0

/** The catalogue page of AiToolApproval. */
object MonarchAiToolApprovalSection : CataloguePage {
    override val title = "AiToolApproval"

    @Composable
    override fun Content() {
        var round by rememberSaveable { mutableIntStateOf(0) }
        var result by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Approve, edit the arguments, or deny with a reason")
        MonarchAiToolApproval(
            part = MonarchAiToolCallPart(
                id = "send-$round",
                name = "send_email",
                input = SampleInput,
                state = MonarchAiToolCallState.PendingApproval,
            ),
            onApprove = { input -> result = if (input == null) "Approved as asked" else "Approved with edits" },
            onDeny = { reason -> result = "Denied" + (reason?.let { ": $it" } ?: "") },
            argumentLabels = mapOf("to" to "Recipients"),
            onAlwaysAllow = {},
            describeTool = { "Send an email to 2 people" },
        )
        if (result.isNotEmpty()) MonarchText(text = result)
        MonarchButton(
            text = "Ask again",
            onClick = {
                round++
                result = ""
            },
        )
        CatalogueCaption("A destructive call")
        MonarchAiToolApproval(
            part = MonarchAiToolCallPart(
                id = "delete-1",
                name = "delete_records",
                input = MonarchAiJSON.Object(mapOf("count" to MonarchAiJSON.Number(SampleRecordCount))),
                state = MonarchAiToolCallState.PendingApproval,
            ),
            onApprove = {},
            onDeny = {},
            risk = MonarchAiToolCallRisk.Destructive,
            describeTool = { "Delete 12 records" },
        )
        CatalogueCaption("Cancelled before anyone decided")
        MonarchAiToolApproval(
            part = MonarchAiToolCallPart(
                id = "cancelled-1",
                name = "search_documents",
                state = MonarchAiToolCallState.Cancelled,
            ),
            onApprove = {},
            onDeny = {},
            risk = MonarchAiToolCallRisk.Low,
        )
    }
}
