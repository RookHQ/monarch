import Foundation

/// A finished move of an event in a ``MonarchTimeGrid`` to a new time or day,
/// by dragging or by a VoiceOver action. The app decides whether to apply or
/// confirm it.
public struct MonarchTimeGridMove: Hashable, Sendable {
    /// The event as it was before the move.
    public let event: MonarchAgendaEvent
    /// The new start.
    public let start: Date
    /// The new end: the new start plus the event's duration.
    public let end: Date

    /// A move.
    ///
    /// - Parameters:
    ///   - event: The event as it was before the move.
    ///   - start: The new start.
    ///   - end: The new end.
    public init(event: MonarchAgendaEvent, start: Date, end: Date) {
        self.event = event
        self.start = start
        self.end = end
    }
}
