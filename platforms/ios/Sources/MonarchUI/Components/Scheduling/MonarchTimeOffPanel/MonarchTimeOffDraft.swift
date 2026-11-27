import Foundation

/// What the add form of ``MonarchTimeOffPanel`` produces.
@_spi(Experimental) public struct MonarchTimeOffDraft: Sendable, Hashable {
    /// The chosen person's value, or `nil` for a closure.
    public var personID: String?
    /// Whether `start` and `end` are whole days.
    public var isAllDay: Bool
    /// The first day (the first moment of it, in the panel's time zone) or the start moment.
    public var start: Date
    /// The last day, included (the first moment of it), or the end moment.
    public var end: Date
    /// The chosen type's value, when types are offered.
    public var type: String?
    /// The trimmed reason, when one is given.
    public var reason: String?

    /// A draft.
    ///
    /// - Parameters:
    ///   - personID: The chosen person, or `nil` for a closure.
    ///   - isAllDay: Whether the dates are whole days.
    ///   - start: The first day or the start moment.
    ///   - end: The last day, included, or the end moment.
    ///   - type: The chosen type's value.
    ///   - reason: The trimmed reason.
    public init(
        personID: String?,
        isAllDay: Bool,
        start: Date,
        end: Date,
        type: String? = nil,
        reason: String? = nil
    ) {
        self.personID = personID
        self.isAllDay = isAllDay
        self.start = start
        self.end = end
        self.type = type
        self.reason = reason
    }
}
