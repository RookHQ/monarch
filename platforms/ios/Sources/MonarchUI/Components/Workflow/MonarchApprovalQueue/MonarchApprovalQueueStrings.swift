/// Built-in strings of the approval queue, from the `MonarchApprovalQueue` strings table.
enum MonarchApprovalQueueStrings {
    private static let table = "MonarchApprovalQueue"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.approvalQueue.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.approvalQueue.\(key)", table: table, argument)
    }

    /// "Requests waiting for you"
    static var label: String { string("label") }
    /// "Approve"
    static var approve: String { string("approve") }
    /// "Send back"
    static var reject: String { string("reject") }
    /// "Review"
    static var review: String { string("review") }
    /// "Approve request from …"
    static func approveItem(_ requester: String) -> String { string("approveItem", requester) }
    /// "Send back request from …"
    static func rejectItem(_ requester: String) -> String { string("rejectItem", requester) }
    /// "Review request from …"
    static func reviewItem(_ requester: String) -> String { string("reviewItem", requester) }
    /// "Select request from …"
    static func selectItem(_ requester: String) -> String { string("selectItem", requester) }
    /// "Select all"
    static var selectAll: String { string("selectAll") }
    /// "Approve selected"
    static var approveSelected: String { string("approveSelected") }
    /// "Past due"
    static var overdue: String { string("overdue") }
    /// "Search by name"
    static var search: String { string("search") }
    /// "Nothing to review"
    static var emptyTitle: String { string("emptyTitle") }
    /// "Requests that need your decision show up here."
    static var emptyDescription: String { string("emptyDescription") }
    /// "No requests match your search"
    static var noMatchesTitle: String { string("noMatchesTitle") }
    /// "Send back the request from …?"
    static func rejectTitle(_ requester: String) -> String { string("rejectTitle", requester) }
    /// "They will be told it was not approved and can change it and resubmit."
    static var rejectDescription: String { string("rejectDescription") }
    /// "Reason"
    static var rejectReason: String { string("rejectReason") }
    /// "Let them know what to change"
    static var rejectReasonPrompt: String { string("rejectReasonPrompt") }
    /// "Each request moves to its next step, or is approved if this was the last one."
    static var bulkDescription: String { string("bulkDescription") }
    /// "Approve all"
    static var bulkConfirm: String { string("bulkConfirm") }
    /// "Cancel"
    static var cancel: String { string("cancel") }
    /// "Approved the request from …."
    static func approved(_ requester: String) -> String { string("approved", requester) }
    /// "Sent back the request from …."
    static func rejected(_ requester: String) -> String { string("rejected", requester) }
    /// "That did not work. Try again."
    static var failed: String { string("failed") }
    /// "Approve … requests?", or the singular for one.
    static func bulkTitle(_ count: Int) -> String {
        string(count == 1 ? "bulkTitle.one" : "bulkTitle.other", count.formatted())
    }
    /// "Approved … requests.", or the singular for one.
    static func bulkApproved(_ count: Int) -> String {
        string(count == 1 ? "bulkApproved.one" : "bulkApproved.other", count.formatted())
    }
}
