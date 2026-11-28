/// A time entry present in both versions of a timesheet with different values.
public struct MonarchTimesheetDiffChange: Sendable, Hashable, Identifiable {
    /// The identifier of the entry.
    public let id: String
    /// The fields that changed, in the order they are named.
    public let fields: [MonarchTimesheetDiffField]
    /// The entry as it was.
    public let before: MonarchTimesheetDiffEntry
    /// The entry as it is now.
    public let after: MonarchTimesheetDiffEntry

    /// A changed entry.
    ///
    /// - Parameters:
    ///   - id: The identifier of the entry.
    ///   - fields: The fields that changed.
    ///   - before: The entry as it was.
    ///   - after: The entry as it is now.
    public init(
        id: String,
        fields: [MonarchTimesheetDiffField],
        before: MonarchTimesheetDiffEntry,
        after: MonarchTimesheetDiffEntry
    ) {
        self.id = id
        self.fields = fields
        self.before = before
        self.after = after
    }
}
