@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewPeople = listOf(
    MonarchAiSharedSessionParticipant(id = "maya", name = "Maya", role = MonarchAiSharedSessionRole.Owner, you = true),
    MonarchAiSharedSessionParticipant(id = "sam", name = "Sam Lee", activity = MonarchAiSharedSessionActivity.Typing),
    MonarchAiSharedSessionParticipant(id = "ra", name = "Research agent", kind = MonarchAiSharedSessionKind.Agent),
)

@MonarchPreviews
@Composable
internal fun MonarchAiSharedSessionPreview() {
    PreviewFrame {
        MonarchAiSharedSession(
            participants = PreviewPeople,
            answering = MonarchAiSharedSessionAnswering("ra", "sam"),
            queue = listOf(MonarchAiSharedSessionRequest("q1", "maya", "Draft the reply")),
            queueExpanded = true,
            onWithdraw = {},
            onInvite = {},
        )
        MonarchAiSharedSession(
            participants = listOf(
                MonarchAiSharedSessionParticipant(
                    id = "v",
                    name = "Vic",
                    role = MonarchAiSharedSessionRole.Viewer,
                    you = true,
                ),
            ),
            onRequestAccess = {},
        )
    }
}
