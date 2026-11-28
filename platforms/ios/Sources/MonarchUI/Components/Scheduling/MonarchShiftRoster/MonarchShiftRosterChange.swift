import Foundation

/// The one change a person made in a ``MonarchShiftRoster``, passed to
/// `onChange` after the value is updated, so the app can save it.
@_spi(Experimental) public struct MonarchShiftRosterChange: Hashable, Sendable {
    /// The `id` of the person.
    public let memberId: String
    /// The start of the day, in the roster's calendar.
    public let day: Date
    /// The new assignment, or `nil` when it was cleared.
    public let assignment: MonarchShiftRosterAssignment?

    /// A change.
    ///
    /// - Parameters:
    ///   - memberId: The `id` of the person.
    ///   - day: The start of the day, in the roster's calendar.
    ///   - assignment: The new assignment, or `nil` when it was cleared.
    public init(memberId: String, day: Date, assignment: MonarchShiftRosterAssignment?) {
        self.memberId = memberId
        self.day = day
        self.assignment = assignment
    }
}
