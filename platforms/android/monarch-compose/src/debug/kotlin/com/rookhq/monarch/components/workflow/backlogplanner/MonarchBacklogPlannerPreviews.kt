@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

private const val PreviewHeightSteps = 6

private class PreviewIssue(val key: String, val title: String, val points: Int?)

private val PreviewBuckets = listOf(
    MonarchBacklogBucket(
        id = "sprint",
        title = "Sprint 14",
        items = listOf(PreviewIssue("ABC-1", "Fix login", 3), PreviewIssue("ABC-2", "Billing page", 5)),
        subtitle = "Nov 10 – Nov 21",
        capacity = 6,
    ),
    MonarchBacklogBucket(id = "next", title = "Sprint 15", items = emptyList<PreviewIssue>()),
    MonarchBacklogBucket(
        id = "backlog",
        title = "Backlog",
        items = listOf(PreviewIssue("ABC-3", "Search", null)),
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchBacklogPlannerPreview() {
    PreviewFrame {
        MonarchBacklogPlanner(
            buckets = PreviewBuckets,
            key = { it.key },
            itemLabel = { it.key },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            itemPoints = { it.points },
            onMove = { _, _ -> },
        ) { issue, _ -> MonarchText(text = issue.title) }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchBacklogPlannerReadOnlyPreview() {
    PreviewFrame {
        MonarchBacklogPlanner(
            buckets = PreviewBuckets,
            key = { it.key },
            itemLabel = { it.key },
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            collapsed = setOf("backlog"),
        ) { issue, _ -> MonarchText(text = issue.title) }
    }
}
