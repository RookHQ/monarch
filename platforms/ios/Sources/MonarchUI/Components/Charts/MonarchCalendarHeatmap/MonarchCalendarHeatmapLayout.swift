/// How a ``MonarchCalendarHeatmap`` arranges its days.
public enum MonarchCalendarHeatmapLayout: Sendable, Hashable, CaseIterable {
    /// A contribution-style grid: weeks as columns and weekdays as rows,
    /// scrolling sideways when it is wider than the screen. The default.
    case year
    /// A month calendar: weeks as rows, with day numbers and values.
    case month
}
