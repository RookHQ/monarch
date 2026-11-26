import Foundation

/// One bookable time of a ``MonarchSlotPicker``.
public struct MonarchSlotPickerSlot: Identifiable, Sendable, Hashable {
    /// The start: also the slot's value and identity.
    public let start: Date
    /// The end, when the app wants it back with the slot. Not shown.
    public let end: Date?
    /// Places left on a group booking. Shown under the time when set.
    public let seatsLeft: Int?
    /// Shown but cannot be chosen, such as a time that was just taken.
    public let isDisabled: Bool

    /// The slot's identity: its start.
    public var id: Date { start }

    /// A bookable time.
    ///
    /// - Parameters:
    ///   - start: The start.
    ///   - end: The end, when the app wants it back with the slot.
    ///   - seatsLeft: Places left on a group booking.
    ///   - isDisabled: Shown but cannot be chosen.
    public init(start: Date, end: Date? = nil, seatsLeft: Int? = nil, isDisabled: Bool = false) {
        self.start = start
        self.end = end
        self.seatsLeft = seatsLeft
        self.isDisabled = isDisabled
    }
}
