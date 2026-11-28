@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aimultiagentroster

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewMembers = listOf(
    MonarchAiMultiAgentRosterMember(
        MonarchAiAgentRef(name = "Coordinator", id = "coordinator", role = "Plans the work"),
        state = MonarchAiAgentState.Working,
        task = "Splitting the request",
    ),
    MonarchAiMultiAgentRosterMember(
        MonarchAiAgentRef(name = "Research agent", id = "research"),
        state = MonarchAiAgentState.Waiting,
        task = "Needs access to the CRM",
    ),
    MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(name = "Writing agent", id = "writer")),
)

@MonarchPreviews
@Composable
internal fun MonarchAiMultiAgentRosterPreview() {
    PreviewFrame {
        MonarchAiMultiAgentRoster(members = PreviewMembers)
        MonarchAiMultiAgentRoster(
            members = PreviewMembers,
            variant = MonarchAiMultiAgentRosterVariant.Stack,
            maxVisible = 1,
        )
        MonarchAiMultiAgentRoster(members = emptyList())
    }
}
