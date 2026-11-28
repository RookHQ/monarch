package com.rookhq.monarch.components.workflow.approvaltimeline

/** Where a step of an approval stands, for `MonarchApprovalTimelineStep`. */
public enum class MonarchApprovalStepStatus {
    /** The request was sent. Usually the first step. */
    Submitted,

    /** Someone approved. */
    Approved,

    /** Someone rejected or sent it back. */
    Rejected,

    /** Waiting for a decision now. */
    Pending,

    /** A later step that has not started. */
    Waiting,

    /** Passed over, for example because the approver is the requester. */
    Skipped,

    /** Withdrawn before a decision. */
    Cancelled,
}
