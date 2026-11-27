/// How a stacked ``MonarchStackedBarChart`` measures its bars.
public enum MonarchStackedBarChartOffset: Sendable, Hashable, CaseIterable {
    /// The values are stacked: a bar is as long as its category's total. The default.
    case normal
    /// Each category's shares of its total are stacked, so every bar is 100%
    /// long. Values, labels, the table and the summary are percentages.
    case expand
}
