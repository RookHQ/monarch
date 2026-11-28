/// How the insights of a ``MonarchInsightStrip`` are arranged.
public enum MonarchInsightStripLayout: Sendable, Hashable, CaseIterable {
    /// One row that scrolls sideways, one card at a time. The default.
    case scroll
    /// One column.
    case stack
    /// As many columns as fit the width.
    case grid
}
