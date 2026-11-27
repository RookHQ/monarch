import Foundation

/// One row of a ``MonarchTimesheetGrid``: a project, task or issue, with the
/// minutes logged on each day.
@_spi(Experimental) public struct MonarchTimesheetGridRow: Sendable, Hashable, Identifiable {
    /// A stable identifier, passed back to `onCellChange`.
    public let id: String
    /// The row's name, such as a project, already localised.
    public let label: String
    /// Secondary text under the name, such as the activity or "Billable".
    public let description: String?
    /// Minutes per day, keyed by the day's `date`. Missing days are empty.
    public let cells: [Date: Int]
    /// Whether the row cannot be typed into, for example time from a timer
    /// or an import.
    public let isReadOnly: Bool

    /// A row of the grid.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - label: The row's name, already localised.
    ///   - description: Secondary text under the name.
    ///   - cells: Minutes per day, keyed by the day's `date`.
    ///   - isReadOnly: Whether the row cannot be typed into.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        cells: [Date: Int] = [:],
        isReadOnly: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.cells = cells
        self.isReadOnly = isReadOnly
    }

    /// The minutes logged on a day, 0 when none.
    ///
    /// - Parameter date: The day's `date`.
    /// - Returns: The minutes in the cell, never negative.
    public func minutes(on date: Date) -> Int {
        max(0, cells[date] ?? 0)
    }
}
