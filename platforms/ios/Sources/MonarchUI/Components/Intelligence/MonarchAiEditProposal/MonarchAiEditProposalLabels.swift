/// Replacements for the built-in strings of ``MonarchAiEditProposal``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiEditProposalLabels: Sendable {
    /// The provenance badge. Built in: "AI generated".
    public var badge: String?
    /// The name of a proposal state. Built in: "Proposed", "Applied", "Rejected", "Reverted".
    public var state: (@Sendable (MonarchAiEditProposalState) -> String)?
    /// The name of a decision. Built in: "Accepted", "Rejected".
    public var hunkState: (@Sendable (MonarchAiEditHunkStatus) -> String)?
    /// The count line from the total and the undecided changes.
    /// Built in: "3 changes", "1 of 3 changes left".
    public var count: (@Sendable (Int, Int) -> String)?
    /// The name of a change without a label, from its position and the
    /// total. Built in: "Change 2 of 3".
    public var hunkName: (@Sendable (Int, Int) -> String)?
    /// Built in: "Accept all".
    public var acceptAll: String?
    /// Built in: "Reject all".
    public var rejectAll: String?
    /// Built in: "Accept".
    public var accept: String?
    /// Built in: "Reject".
    public var reject: String?
    /// Built in: "Revert".
    public var revert: String?
    /// Read before removed text. Built in: "Deleted:".
    public var deleted: String?
    /// Read before added text. Built in: "Inserted:".
    public var inserted: String?
    /// Shown after a change that changes nothing. Built in: "No change".
    public var unchanged: String?
    /// Announced after a decision, from the event and the changes left.
    /// Built in: "Change accepted. 2 left.", "All changes rejected."
    public var announcement: (@Sendable (MonarchAiEditProposalEvent, Int) -> String)?
    /// The note once the proposal is no longer proposed. Built in: "These
    /// edits were applied.", "…rejected.", "…reverted."
    public var readOnly: (@Sendable (MonarchAiEditProposalState) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - badge: The provenance badge.
    ///   - state: The name of a proposal state.
    ///   - hunkState: The name of a decision.
    ///   - count: The count line.
    ///   - hunkName: The name of a change without a label.
    ///   - acceptAll: The accept all button.
    ///   - rejectAll: The reject all button.
    ///   - accept: The accept button of a change.
    ///   - reject: The reject button of a change.
    ///   - revert: The revert button.
    ///   - deleted: Read before removed text.
    ///   - inserted: Read before added text.
    ///   - unchanged: Shown after a change that changes nothing.
    ///   - announcement: Announced after a decision.
    ///   - readOnly: The note once the proposal is no longer proposed.
    public init(
        badge: String? = nil,
        state: (@Sendable (MonarchAiEditProposalState) -> String)? = nil,
        hunkState: (@Sendable (MonarchAiEditHunkStatus) -> String)? = nil,
        count: (@Sendable (Int, Int) -> String)? = nil,
        hunkName: (@Sendable (Int, Int) -> String)? = nil,
        acceptAll: String? = nil,
        rejectAll: String? = nil,
        accept: String? = nil,
        reject: String? = nil,
        revert: String? = nil,
        deleted: String? = nil,
        inserted: String? = nil,
        unchanged: String? = nil,
        announcement: (@Sendable (MonarchAiEditProposalEvent, Int) -> String)? = nil,
        readOnly: (@Sendable (MonarchAiEditProposalState) -> String)? = nil
    ) {
        self.badge = badge
        self.state = state
        self.hunkState = hunkState
        self.count = count
        self.hunkName = hunkName
        self.acceptAll = acceptAll
        self.rejectAll = rejectAll
        self.accept = accept
        self.reject = reject
        self.revert = revert
        self.deleted = deleted
        self.inserted = inserted
        self.unchanged = unchanged
        self.announcement = announcement
        self.readOnly = readOnly
    }
}
