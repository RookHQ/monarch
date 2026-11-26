import Foundation

/// An empty slot of a ``MonarchTimeGrid`` that the person chose, to create an
/// event there.
public struct MonarchTimeGridSlot: Hashable, Sendable {
    /// The first moment of the day column, in the grid's calendar.
    public let day: Date
    /// Wall-clock minutes from midnight, snapped to the grid's `snapMinutes`.
    public let minutes: Int
    /// The moment the slot starts, in the grid's calendar and time zone.
    public let start: Date

    /// A slot.
    ///
    /// - Parameters:
    ///   - day: The first moment of the day column.
    ///   - minutes: Wall-clock minutes from midnight.
    ///   - start: The moment the slot starts.
    public init(day: Date, minutes: Int, start: Date) {
        self.day = day
        self.minutes = minutes
        self.start = start
    }
}
