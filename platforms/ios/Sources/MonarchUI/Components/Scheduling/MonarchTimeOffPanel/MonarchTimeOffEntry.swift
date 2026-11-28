import Foundation

/// One period of time off in ``MonarchTimeOffPanel``: a holiday, sickness,
/// training, or a closure that applies to everyone.
@_spi(Experimental) public struct MonarchTimeOffEntry: Identifiable, Sendable, Hashable {
    /// A stable key.
    public var id: String
    /// Whose time off, or `nil` for a closure that applies to everyone.
    public var personID: String?
    /// The name shown for the person, already localised.
    public var personName: String?
    /// Whether `start` and `end` are whole days (the end day included) or moments.
    public var isAllDay: Bool
    /// The first day (all day) or the first moment.
    public var start: Date
    /// The last day, included (all day), or the end moment.
    public var end: Date
    /// The kind, such as "Annual leave". Shown as a badge.
    public var type: String?
    /// A note in the person's own words, such as "Conference".
    public var reason: String?
    /// A status shown as a badge, such as "Pending".
    public var status: String?

    /// A period of time off.
    ///
    /// - Parameters:
    ///   - id: A stable key.
    ///   - personID: Whose time off, or `nil` for a closure.
    ///   - personName: The name shown for the person.
    ///   - isAllDay: Whether the dates are whole days.
    ///   - start: The first day or moment.
    ///   - end: The last day, included, or the end moment.
    ///   - type: The kind, shown as a badge.
    ///   - reason: A note shown after the dates.
    ///   - status: A status shown as a badge.
    public init(
        id: String,
        personID: String?,
        personName: String? = nil,
        isAllDay: Bool,
        start: Date,
        end: Date,
        type: String? = nil,
        reason: String? = nil,
        status: String? = nil
    ) {
        self.id = id
        self.personID = personID
        self.personName = personName
        self.isAllDay = isAllDay
        self.start = start
        self.end = end
        self.type = type
        self.reason = reason
        self.status = status
    }
}
