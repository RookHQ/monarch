import Foundation

/// The dates of an item of a ``MonarchGanttTimeline``. With only one of the
/// two the item is a one-day marker.
@_spi(Experimental) public struct MonarchGanttTimelineDates: Hashable, Sendable {
    /// The first day, or `nil`.
    public var start: Date?
    /// The last day, inclusive, or `nil`.
    public var end: Date?

    /// Dates.
    ///
    /// - Parameters:
    ///   - start: The first day, or `nil`.
    ///   - end: The last day, inclusive, or `nil`.
    public init(start: Date?, end: Date?) {
        self.start = start
        self.end = end
    }

    /// These dates at the start of their days, moved by whole days with the
    /// rules the timeline uses for a drag, a key press or a VoiceOver action.
    /// ``MonarchGanttTimelineShift/start`` and ``MonarchGanttTimelineShift/end``
    /// never let the first day pass the last.
    ///
    /// - Parameters:
    ///   - shift: Which dates move.
    ///   - days: How many days, negative for earlier.
    ///   - calendar: The calendar the days are counted in.
    /// - Returns: The new dates.
    public func shifted(
        _ shift: MonarchGanttTimelineShift,
        by days: Int,
        in calendar: Calendar = .autoupdatingCurrent
    ) -> MonarchGanttTimelineDates {
        let first = start.map(calendar.startOfDay(for:))
        let last = end.map(calendar.startOfDay(for:))
        func add(_ date: Date?) -> Date? {
            date.flatMap { calendar.date(byAdding: .day, value: days, to: $0) }
        }
        guard days != 0 else { return MonarchGanttTimelineDates(start: first, end: last) }
        switch shift {
        case .move:
            return MonarchGanttTimelineDates(start: add(first), end: add(last))
        case .start:
            let moved = add(first ?? last).map { min($0, last ?? $0) }
            return MonarchGanttTimelineDates(start: moved, end: last)
        case .end:
            let moved = add(last ?? first).map { max($0, first ?? $0) }
            return MonarchGanttTimelineDates(start: first, end: moved)
        }
    }
}
