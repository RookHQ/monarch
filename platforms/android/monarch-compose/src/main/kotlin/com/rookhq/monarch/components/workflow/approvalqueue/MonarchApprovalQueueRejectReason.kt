package com.rookhq.monarch.components.workflow.approvalqueue

/** Whether sending a request back in a `MonarchApprovalQueue` asks for a reason. */
public enum class MonarchApprovalQueueRejectReason {
    /** The dialog has a reason field, and Send back stays disabled while it is empty. */
    Required,

    /** The dialog has a reason field that may stay empty. */
    Optional,

    /** The dialog asks for confirmation only; the reason passed back is `null`. */
    None,
}
