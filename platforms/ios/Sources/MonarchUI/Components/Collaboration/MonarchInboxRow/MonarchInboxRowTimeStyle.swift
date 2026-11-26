/// How a ``MonarchInboxRow`` writes its time.
public enum MonarchInboxRowTimeStyle: Sendable, Hashable, CaseIterable {
    /// A short age for dense lists: "now", "5m", "3h", "2d", "4w", then a short date.
    case compact
    /// The full relative time: "5 minutes ago", "yesterday", as the web notification centre writes it.
    case relative
}
