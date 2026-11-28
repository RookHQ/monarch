/// How much time the visible window of a ``MonarchGanttTimeline`` covers, and
/// how far Earlier and Later move it.
@_spi(Experimental) public enum MonarchGanttTimelineZoom: String, Sendable, Hashable, CaseIterable {
    /// Four weeks, a column per day. Earlier and Later move a week.
    case week
    /// Thirteen weeks. Earlier and Later move thirty days.
    case month
    /// Nine months. Earlier and Later move a quarter.
    case quarter
    /// Two years. Earlier and Later move half a year.
    case year
}
