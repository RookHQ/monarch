@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aimultiagentroster

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
