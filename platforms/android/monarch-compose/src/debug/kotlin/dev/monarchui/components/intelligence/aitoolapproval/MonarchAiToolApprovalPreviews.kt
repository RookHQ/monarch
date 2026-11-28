@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aitoolapproval

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewCall = MonarchAiToolCallPart(
    id = "call-1",
    name = "send_email",
    input = MonarchAiJSON.Object(
        mapOf(
            "to" to MonarchAiJSON.Array(
                listOf(MonarchAiJSON.Text("ada@example.com"), MonarchAiJSON.Text("ben@example.com")),
            ),
            "subject" to MonarchAiJSON.Text("Q3 plan"),
        ),
    ),
    state = MonarchAiToolCallState.PendingApproval,
)

@MonarchPreviews
@Composable
internal fun MonarchAiToolApprovalPreview() {
    PreviewFrame {
        MonarchAiToolApproval(part = PreviewCall, onApprove = {}, onDeny = {}, onAlwaysAllow = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiToolApprovalDestructivePreview() {
    PreviewFrame {
        MonarchAiToolApproval(
            part = PreviewCall.copy(id = "call-2", name = "delete_records", input = MonarchAiJSON.Null),
            onApprove = {},
            onDeny = {},
            risk = MonarchAiToolCallRisk.Destructive,
        )
        MonarchAiToolApproval(
            part = PreviewCall.copy(id = "call-3", state = MonarchAiToolCallState.Denied),
            onApprove = {},
            onDeny = {},
        )
    }
}
