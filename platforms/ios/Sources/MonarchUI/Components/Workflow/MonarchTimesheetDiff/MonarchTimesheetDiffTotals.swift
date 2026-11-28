/// The minutes a timesheet gained (positive) or lost (negative) between two
/// versions.
public struct MonarchTimesheetDiffTotals: Sendable, Hashable {
    /// The change of the total time, in minutes.
    public let total: Int
    /// The change of the billable time, in minutes. Entries whose
    /// `isBillable` is `nil` count as billable, as on the web.
    public let billable: Int

    /// Totals.
    ///
    /// - Parameters:
    ///   - total: The change of the total time, in minutes.
    ///   - billable: The change of the billable time, in minutes.
    public init(total: Int, billable: Int) {
        self.total = total
        self.billable = billable
    }
}
