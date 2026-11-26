/// How an SLA indicator is drawn.
public enum MonarchSlaIndicatorVariant: Sendable, Hashable, CaseIterable {
    /// A compact chip, for lists and headers. The default.
    case chip
    /// An icon and text without a surface, for rows of a detail panel.
    case text
}
