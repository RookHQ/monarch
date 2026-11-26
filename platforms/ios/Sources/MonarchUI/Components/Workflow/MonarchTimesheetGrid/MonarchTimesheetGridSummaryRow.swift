import Foundation

/// An extra footer row of a ``MonarchTimesheetGrid`` with a display-only
/// value per day, such as attendance or clock-in times.
@_spi(Experimental) public struct MonarchTimesheetGridSummaryRow: Sendable, Hashable, Identifiable {
    /// A stable identifier.
    public let id: String
    /// The row's name, already localised.
    public let label: String
    /// The text per day, keyed by the day's `date`, already localised.
    public let cells: [Date: String]
    /// The text in the total column, if any.
    public let total: String?

    /// A summary row.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - label: The row's name, already localised.
    ///   - cells: The text per day, keyed by the day's `date`.
    ///   - total: The text in the total column.
    public init(id: String, label: String, cells: [Date: String], total: String? = nil) {
        self.id = id
        self.label = label
        self.cells = cells
        self.total = total
    }
}
