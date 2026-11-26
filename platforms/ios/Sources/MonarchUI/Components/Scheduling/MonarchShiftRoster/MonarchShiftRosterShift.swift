import SwiftUI

/// A shift that can be assigned in a ``MonarchShiftRoster``, such as
/// "Morning, 06:00 to 14:00".
///
/// ```swift
/// MonarchShiftRosterShift(id: "n", name: "Night", start: 22 * 60, end: 6 * 60, shortName: "N")
/// ```
@_spi(Experimental) public struct MonarchShiftRosterShift: Identifiable, Sendable {
    /// A stable identifier, unique among the roster's shifts. Assignments refer to it.
    public let id: String
    /// The shift's name, already localised: "Morning".
    public var name: String
    /// When the shift starts, in minutes from midnight (0 to 1439).
    public var start: Int
    /// When it ends, in minutes from midnight. An end at or before `start`
    /// ends the next day: the shift is overnight.
    public var end: Int
    /// A short name for the narrow cells of the grid, such as "M". `nil` uses `name`.
    public var shortName: String?
    /// The shift's colour, for the bar and tint of its cells only. `nil` is a neutral grey.
    public var color: Color?

    /// A shift.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, unique among the roster's shifts.
    ///   - name: The shift's name, already localised.
    ///   - start: When it starts, in minutes from midnight.
    ///   - end: When it ends, in minutes from midnight; at or before `start` is the next day.
    ///   - shortName: A short name for the grid's narrow cells.
    ///   - color: The shift's colour for the bar and tint. `nil` is a neutral grey.
    public init(id: String, name: String, start: Int, end: Int, shortName: String? = nil, color: Color? = nil) {
        self.id = id
        self.name = name
        self.start = start
        self.end = end
        self.shortName = shortName
        self.color = color
    }
}
