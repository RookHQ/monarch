/// Where a draft is in review, for ``MonarchAiDraftQueue``.
public enum MonarchAiDraftStatus: String, Sendable, Hashable, CaseIterable {
    /// Waiting for a decision.
    case draft
    /// A new version is being written.
    case regenerating
    /// Approved, and waiting to be sent when the queue has a send action.
    case approved
    /// Sent.
    case sent
    /// Skipped without sending.
    case skipped
}
