/// Which dates of a ``MonarchGanttTimeline`` item a drag, a key press or a
/// VoiceOver action moves.
@_spi(Experimental) public enum MonarchGanttTimelineShift: String, Sendable, Hashable, CaseIterable {
    /// Both dates: the whole bar moves.
    case move
    /// Only the first day.
    case start
    /// Only the last day.
    case end
}
