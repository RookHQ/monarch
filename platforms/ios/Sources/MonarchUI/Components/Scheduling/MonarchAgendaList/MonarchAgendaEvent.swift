import SwiftUI

/// One event in a ``MonarchAgendaList``.
///
/// ```swift
/// MonarchAgendaEvent("Design review", id: "e1", start: start, end: end, detail: "Room 4")
/// MonarchAgendaEvent("Company offsite", id: "e2", start: firstDay, end: lastDay, isAllDay: true)
/// ```
public struct MonarchAgendaEvent: Identifiable, Hashable, Sendable {
    /// The identity of the event, unique in the list.
    public let id: String
    /// The title, already localised.
    public var title: String
    /// When the event starts. For an all-day event, any time on its first day.
    public var start: Date
    /// When a timed event ends, or any time on the last day of an all-day event. `nil` for a
    /// moment with no end.
    public var end: Date?
    /// Whether the event takes whole days.
    public var isAllDay: Bool
    /// A second, muted line: a place, a service, an organiser.
    public var detail: String?
    /// The event's colour for the chip's bar. The default is a neutral grey.
    public var color: Color?
    /// Confirmed, tentative, cancelled or declined.
    public var status: MonarchEventChipStatus

    /// An event.
    ///
    /// - Parameters:
    ///   - title: The title, already localised.
    ///   - id: The identity of the event, unique in the list.
    ///   - start: When the event starts; for an all-day event, any time on its first day.
    ///   - end: When a timed event ends (exclusive), or any time on the last day of an all-day
    ///     event (inclusive).
    ///   - isAllDay: Whether the event takes whole days.
    ///   - detail: A second, muted line.
    ///   - color: The event's colour.
    ///   - status: Confirmed, tentative, cancelled or declined.
    public init(
        _ title: String,
        id: String,
        start: Date,
        end: Date? = nil,
        isAllDay: Bool = false,
        detail: String? = nil,
        color: Color? = nil,
        status: MonarchEventChipStatus = .confirmed
    ) {
        self.title = title
        self.id = id
        self.start = start
        self.end = end
        self.isAllDay = isAllDay
        self.detail = detail
        self.color = color
        self.status = status
    }
}
