@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow.approvalqueue

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewHour = 3_600_000L
private const val PreviewHeightSteps = 5

private val PreviewItems = listOf(
    MonarchApprovalQueueItem(
        id = "1",
        requesterName = "Alex Morgan",
        title = "Expense claim",
        requesterDetail = "Design",
        summary = "3 receipts, travel",
        submittedMillis = PreviewNow - 48 * PreviewHour,
        overdue = true,
    ),
    MonarchApprovalQueueItem(
        id = "2",
        requesterName = "Sam Lee",
        title = "Leave, 2 days",
        submittedMillis = PreviewNow - PreviewHour,
    ),
    MonarchApprovalQueueItem(id = "3", requesterName = "Jo Park", title = "Timesheet", canDecide = false),
)

@MonarchPreviews
@Composable
internal fun MonarchApprovalQueuePreview() {
    PreviewFrame {
        MonarchApprovalQueue(
            items = PreviewItems,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            now = PreviewNow,
            onApprove = {},
            onReject = { _, _ -> },
            onReview = {},
            onApproveMany = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchApprovalQueueEmptyPreview() {
    PreviewFrame {
        MonarchApprovalQueue(items = emptyList(), searchable = true, onApprove = {})
    }
}
