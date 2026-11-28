package dev.monarchui.components.workflow.approvalqueue

/** What the rows of a MonarchApprovalQueue can do; `null` hides the action. */
internal class MonarchApprovalQueueRowActions(
    val onApprove: ((MonarchApprovalQueueItem) -> Unit)?,
    val onReject: ((MonarchApprovalQueueItem) -> Unit)?,
    val onReview: ((MonarchApprovalQueueItem) -> Unit)?,
    val onSelectedChange: ((MonarchApprovalQueueItem, Boolean) -> Unit)?,
)
