/// Replacements for the built-in strings of ``MonarchApprovalQueue``.
/// Every value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchApprovalQueueLabels: Sendable {
    /// Built in: "Requests waiting for you".
    public var label: String
    /// Built in: "Approve".
    public var approve: String
    /// Built in: "Send back".
    public var reject: String
    /// Built in: "Review".
    public var review: String
    /// From the requester's name. Built in: "Approve request from …".
    public var approveItem: @Sendable (_ requester: String) -> String
    /// From the requester's name. Built in: "Send back request from …".
    public var rejectItem: @Sendable (_ requester: String) -> String
    /// From the requester's name. Built in: "Review request from …".
    public var reviewItem: @Sendable (_ requester: String) -> String
    /// From the requester's name. Built in: "Select request from …".
    public var selectItem: @Sendable (_ requester: String) -> String
    /// Built in: "Select all".
    public var selectAll: String
    /// Built in: "Approve selected".
    public var approveSelected: String
    /// Built in: "Past due".
    public var overdue: String
    /// Built in: "Search by name".
    public var search: String
    /// Built in: "Nothing to review".
    public var emptyTitle: String
    /// Built in: "Requests that need your decision show up here."
    public var emptyDescription: String
    /// Built in: "No requests match your search".
    public var noMatchesTitle: String
    /// From the requester's name. Built in: "Send back the request from …?"
    public var rejectTitle: @Sendable (_ requester: String) -> String
    /// Built in: "They will be told it was not approved and can change it and resubmit."
    public var rejectDescription: String
    /// Built in: "Reason".
    public var rejectReason: String
    /// Built in: "Let them know what to change".
    public var rejectReasonPrompt: String
    /// Built in: "Each request moves to its next step, or is approved if this was the last one."
    public var bulkDescription: String
    /// Built in: "Approve all".
    public var bulkConfirm: String
    /// Built in: "Cancel".
    public var cancel: String
    /// From the requester's name. Built in: "Approved the request from …."
    public var approved: @Sendable (_ requester: String) -> String
    /// From the requester's name. Built in: "Sent back the request from …."
    public var rejected: @Sendable (_ requester: String) -> String
    /// Built in: "That did not work. Try again."
    public var failed: String
    /// From the number of requests. Built in: "Approve … requests?"
    public var bulkTitle: @Sendable (_ count: Int) -> String
    /// From the number of requests. Built in: "Approved … requests."
    public var bulkApproved: @Sendable (_ count: Int) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Requests waiting for you".
    ///   - approve: Replaces "Approve".
    ///   - reject: Replaces "Send back".
    ///   - review: Replaces "Review".
    ///   - approveItem: Replaces "Approve request from …".
    ///   - rejectItem: Replaces "Send back request from …".
    ///   - reviewItem: Replaces "Review request from …".
    ///   - selectItem: Replaces "Select request from …".
    ///   - selectAll: Replaces "Select all".
    ///   - approveSelected: Replaces "Approve selected".
    ///   - overdue: Replaces "Past due".
    ///   - search: Replaces "Search by name".
    ///   - emptyTitle: Replaces "Nothing to review".
    ///   - emptyDescription: Replaces "Requests that need your decision show up here."
    ///   - noMatchesTitle: Replaces "No requests match your search".
    ///   - rejectTitle: Replaces "Send back the request from …?"
    ///   - rejectDescription: Replaces "They will be told it was not approved and can change it and resubmit."
    ///   - rejectReason: Replaces "Reason".
    ///   - rejectReasonPrompt: Replaces "Let them know what to change".
    ///   - bulkDescription: Replaces "Each request moves to its next step, or is approved if this was the last one."
    ///   - bulkConfirm: Replaces "Approve all".
    ///   - cancel: Replaces "Cancel".
    ///   - approved: Replaces "Approved the request from …."
    ///   - rejected: Replaces "Sent back the request from …."
    ///   - failed: Replaces "That did not work. Try again."
    ///   - bulkTitle: Replaces "Approve … requests?"
    ///   - bulkApproved: Replaces "Approved … requests."
    public init(
        label: String? = nil,
        approve: String? = nil,
        reject: String? = nil,
        review: String? = nil,
        approveItem: (@Sendable (_ requester: String) -> String)? = nil,
        rejectItem: (@Sendable (_ requester: String) -> String)? = nil,
        reviewItem: (@Sendable (_ requester: String) -> String)? = nil,
        selectItem: (@Sendable (_ requester: String) -> String)? = nil,
        selectAll: String? = nil,
        approveSelected: String? = nil,
        overdue: String? = nil,
        search: String? = nil,
        emptyTitle: String? = nil,
        emptyDescription: String? = nil,
        noMatchesTitle: String? = nil,
        rejectTitle: (@Sendable (_ requester: String) -> String)? = nil,
        rejectDescription: String? = nil,
        rejectReason: String? = nil,
        rejectReasonPrompt: String? = nil,
        bulkDescription: String? = nil,
        bulkConfirm: String? = nil,
        cancel: String? = nil,
        approved: (@Sendable (_ requester: String) -> String)? = nil,
        rejected: (@Sendable (_ requester: String) -> String)? = nil,
        failed: String? = nil,
        bulkTitle: (@Sendable (_ count: Int) -> String)? = nil,
        bulkApproved: (@Sendable (_ count: Int) -> String)? = nil
    ) {
        self.label = label ?? MonarchApprovalQueueStrings.label
        self.approve = approve ?? MonarchApprovalQueueStrings.approve
        self.reject = reject ?? MonarchApprovalQueueStrings.reject
        self.review = review ?? MonarchApprovalQueueStrings.review
        self.approveItem = approveItem ?? { MonarchApprovalQueueStrings.approveItem($0) }
        self.rejectItem = rejectItem ?? { MonarchApprovalQueueStrings.rejectItem($0) }
        self.reviewItem = reviewItem ?? { MonarchApprovalQueueStrings.reviewItem($0) }
        self.selectItem = selectItem ?? { MonarchApprovalQueueStrings.selectItem($0) }
        self.selectAll = selectAll ?? MonarchApprovalQueueStrings.selectAll
        self.approveSelected = approveSelected ?? MonarchApprovalQueueStrings.approveSelected
        self.overdue = overdue ?? MonarchApprovalQueueStrings.overdue
        self.search = search ?? MonarchApprovalQueueStrings.search
        self.emptyTitle = emptyTitle ?? MonarchApprovalQueueStrings.emptyTitle
        self.emptyDescription = emptyDescription ?? MonarchApprovalQueueStrings.emptyDescription
        self.noMatchesTitle = noMatchesTitle ?? MonarchApprovalQueueStrings.noMatchesTitle
        self.rejectTitle = rejectTitle ?? { MonarchApprovalQueueStrings.rejectTitle($0) }
        self.rejectDescription = rejectDescription ?? MonarchApprovalQueueStrings.rejectDescription
        self.rejectReason = rejectReason ?? MonarchApprovalQueueStrings.rejectReason
        self.rejectReasonPrompt = rejectReasonPrompt ?? MonarchApprovalQueueStrings.rejectReasonPrompt
        self.bulkDescription = bulkDescription ?? MonarchApprovalQueueStrings.bulkDescription
        self.bulkConfirm = bulkConfirm ?? MonarchApprovalQueueStrings.bulkConfirm
        self.cancel = cancel ?? MonarchApprovalQueueStrings.cancel
        self.approved = approved ?? { MonarchApprovalQueueStrings.approved($0) }
        self.rejected = rejected ?? { MonarchApprovalQueueStrings.rejected($0) }
        self.failed = failed ?? MonarchApprovalQueueStrings.failed
        self.bulkTitle = bulkTitle ?? { MonarchApprovalQueueStrings.bulkTitle($0) }
        self.bulkApproved = bulkApproved ?? { MonarchApprovalQueueStrings.bulkApproved($0) }
    }
}
