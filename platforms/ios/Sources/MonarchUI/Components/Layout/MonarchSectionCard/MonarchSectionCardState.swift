/// What the body of a ``MonarchSectionCard`` shows.
public enum MonarchSectionCardState: Sendable, Hashable, CaseIterable {
    /// The content.
    case ready
    /// Skeleton rows while the content loads.
    case loading
    /// An empty state: there is nothing to show yet.
    case empty
    /// An error state, with a retry when the card has `onRetry`.
    case error
}
