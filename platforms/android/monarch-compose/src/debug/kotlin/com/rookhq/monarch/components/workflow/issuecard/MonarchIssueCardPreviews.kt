@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow.issuecard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.prioritypicker.MonarchPriorityLevel
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewStatuses = listOf(
    MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted),
    MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started),
    MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed),
)

/** 12 March 2026 and 20 March 2026, in UTC. */
private const val PreviewDue = 1_773_273_600_000L
private const val PreviewToday = 1_773_964_800_000L

@MonarchPreviews
@Composable
internal fun MonarchIssueCardPreview() {
    PreviewFrame {
        MonarchIssueCard(
            title = "Sign-in fails on slow networks",
            key = "STR-142",
            status = PreviewStatuses[1],
            statuses = PreviewStatuses,
            priority = MonarchPriorityLevel.High,
            assignee = MonarchIssueCardAssignee.Person("Ana Ruiz"),
            tags = listOf("Bug", "Auth"),
            dueDate = PreviewDue,
            today = PreviewToday,
            onStatusChange = {},
            onPriorityChange = {},
            onOpen = {},
        )
        MonarchIssueCard(
            title = "Write the release notes",
            key = "STR-150",
            status = PreviewStatuses[0],
            priority = MonarchPriorityLevel.Low,
            assignee = MonarchIssueCardAssignee.Unassigned,
            selected = true,
        )
        MonarchIssueCard(
            title = "Move the board column",
            key = "STR-151",
            status = PreviewStatuses[2],
            dueDate = PreviewDue,
            today = PreviewToday,
            dragging = true,
        )
    }
}
