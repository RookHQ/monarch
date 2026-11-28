/// Built-in strings of the approval timeline, from the `MonarchApprovalTimeline` strings table.
enum MonarchApprovalTimelineStrings {
    private static let table = "MonarchApprovalTimeline"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.approvalTimeline.\(key)", table: table)
    }

    /// The spoken name of the timeline.
    static var label: String { string("label") }
    /// Spoken on the step waiting for a decision now.
    static var current: String { string("current") }
    /// Shown when there are no steps.
    static var empty: String { string("empty") }

    /// The status name after a step's label.
    static func status(_ status: MonarchApprovalStepStatus) -> String {
        switch status {
        case .submitted: string("status.submitted")
        case .approved: string("status.approved")
        case .rejected: string("status.rejected")
        case .pending: string("status.pending")
        case .waiting: string("status.waiting")
        case .skipped: string("status.skipped")
        case .cancelled: string("status.cancelled")
        }
    }

    /// "Decided by …".
    static func approvers(_ names: String) -> String {
        MonarchStrings.string("monarch.approvalTimeline.approvers", table: table, names)
    }

    /// "standing in for …".
    static func delegatedFrom(_ name: String) -> String {
        MonarchStrings.string("monarch.approvalTimeline.delegatedFrom", table: table, name)
    }
}
