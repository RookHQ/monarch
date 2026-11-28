/// Where the top-level items without dates of a ``MonarchGanttTimeline`` go.
@_spi(Experimental) public enum MonarchGanttTimelineUndated: String, Sendable, Hashable, CaseIterable {
    /// In an Unscheduled disclosure group under the chart.
    case list
    /// As chart rows that say "No dates".
    case row
}
