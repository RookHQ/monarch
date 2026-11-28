/// Replacements for the built-in strings of ``MonarchApprovalTimeline``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchApprovalTimelineLabels: Sendable {
    /// The spoken name of the timeline.
    public var label: String
    /// The status text after each step's label.
    public var status: @Sendable (_ status: MonarchApprovalStepStatus) -> String
    /// The line naming who can decide a pending or waiting step: "Decided by Ana, Ben".
    public var approvers: @Sendable (_ names: String) -> String
    /// The text after an approver who stands in for someone: "standing in for Sam".
    public var delegatedFrom: @Sendable (_ name: String) -> String
    /// Spoken on the step waiting for a decision now.
    public var current: String
    /// Shown when there are no steps.
    public var empty: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Approval timeline".
    ///   - status: Replaces the status names, such as "Approved" and "Up next".
    ///   - approvers: Replaces "Decided by …".
    ///   - delegatedFrom: Replaces "standing in for …".
    ///   - current: Replaces "Current step".
    ///   - empty: Replaces "Not submitted yet".
    public init(
        label: String? = nil,
        status: (@Sendable (_ status: MonarchApprovalStepStatus) -> String)? = nil,
        approvers: (@Sendable (_ names: String) -> String)? = nil,
        delegatedFrom: (@Sendable (_ name: String) -> String)? = nil,
        current: String? = nil,
        empty: String? = nil
    ) {
        self.label = label ?? MonarchApprovalTimelineStrings.label
        self.status = status ?? { MonarchApprovalTimelineStrings.status($0) }
        self.approvers = approvers ?? { MonarchApprovalTimelineStrings.approvers($0) }
        self.delegatedFrom = delegatedFrom ?? { MonarchApprovalTimelineStrings.delegatedFrom($0) }
        self.current = current ?? MonarchApprovalTimelineStrings.current
        self.empty = empty ?? MonarchApprovalTimelineStrings.empty
    }
}
