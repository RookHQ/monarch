package com.rookhq.monarch.components.workflow.approvaltimeline

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val Now = 1_772_636_400_000L
private const val Hour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchApprovalTimelinePreview() {
    PreviewFrame {
        MonarchApprovalTimeline(
            steps = listOf(
                MonarchApprovalTimelineStep(
                    id = "0",
                    label = "Submitted",
                    status = MonarchApprovalStepStatus.Submitted,
                    actor = MonarchApprovalPerson("Sam Lee"),
                    date = Now - 48 * Hour,
                ),
                MonarchApprovalTimelineStep(
                    id = "1",
                    label = "Reporting manager",
                    status = MonarchApprovalStepStatus.Approved,
                    actor = MonarchApprovalPerson("Ana Ruiz"),
                    date = Now - Hour,
                    comment = "Fine by me.",
                ),
                MonarchApprovalTimelineStep(
                    id = "2",
                    label = "Finance check",
                    status = MonarchApprovalStepStatus.Pending,
                    approvers = listOf(MonarchApprovalPerson("Jo Park")),
                    delegatedFrom = MonarchApprovalPerson("Kim Cho"),
                ),
                MonarchApprovalTimelineStep(id = "3", label = "Payout", status = MonarchApprovalStepStatus.Waiting),
            ),
            now = Now,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchApprovalTimelineOutcomesPreview() {
    PreviewFrame {
        MonarchApprovalTimeline(
            steps = listOf(
                MonarchApprovalTimelineStep(
                    id = "0",
                    label = "Legal review",
                    status = MonarchApprovalStepStatus.Rejected,
                    actor = MonarchApprovalPerson("Ben Okafor"),
                    date = Now - 10 * 24 * Hour,
                    comment = "The contract term is missing.",
                ),
                MonarchApprovalTimelineStep(
                    id = "1",
                    label = "Security review",
                    status = MonarchApprovalStepStatus.Skipped,
                    note = "Not needed under 1,000 EUR.",
                ),
                MonarchApprovalTimelineStep(
                    id = "2",
                    label = "Payout",
                    status = MonarchApprovalStepStatus.Cancelled,
                    note = "Withdrawn by the requester.",
                ),
            ),
            now = Now,
            showAvatars = false,
        )
        MonarchApprovalTimeline(steps = emptyList())
    }
}
