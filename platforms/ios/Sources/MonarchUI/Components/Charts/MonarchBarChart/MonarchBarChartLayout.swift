/// How a ``MonarchBarChart`` places several series in one category.
public enum MonarchBarChartLayout: Sendable, Hashable, CaseIterable {
    /// Side by side. The default.
    case grouped
    /// One bar per category, with the series stacked; positive values stack
    /// up from zero and negative values down from it.
    case stacked
}
