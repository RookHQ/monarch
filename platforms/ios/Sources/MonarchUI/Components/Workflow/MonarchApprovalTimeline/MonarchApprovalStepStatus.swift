/// Where a step of an approval stands, for ``MonarchApprovalTimelineStep``.
public enum MonarchApprovalStepStatus: Sendable, Hashable, CaseIterable {
    /// The request was sent. Usually the first step.
    case submitted
    /// Someone approved.
    case approved
    /// Someone rejected or sent it back.
    case rejected
    /// Waiting for a decision now.
    case pending
    /// A later step that has not started.
    case waiting
    /// Passed over, for example because the approver is the requester.
    case skipped
    /// Withdrawn before a decision.
    case cancelled
}
