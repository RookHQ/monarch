/// What a ``MonarchGanttTimeline`` shows: the chart or the list.
@_spi(Experimental) public enum MonarchGanttTimelineView: String, Sendable, Hashable, CaseIterable {
    /// Bars on a calendar.
    case chart
    /// A row per item that writes every date in words, with Change dates.
    case list
}
