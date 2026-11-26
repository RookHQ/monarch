/// The name of a field of a time entry that changed. The built-in fields
/// are compared by ``MonarchTimesheetDiffResult/init(before:after:)``; a
/// product that compares more fields makes its own, such as
/// `MonarchTimesheetDiffField("rate")`.
public struct MonarchTimesheetDiffField: RawRepresentable, Sendable, Hashable {
    /// The field's name.
    public let rawValue: String

    /// A field with a name.
    ///
    /// - Parameter rawValue: The field's name.
    public init(rawValue: String) {
        self.rawValue = rawValue
    }

    /// A field with a name.
    ///
    /// - Parameter name: The field's name.
    public init(_ name: String) {
        self.rawValue = name
    }

    /// The day worked.
    public static let date = MonarchTimesheetDiffField("date")
    /// The time worked.
    public static let minutes = MonarchTimesheetDiffField("minutes")
    /// The project.
    public static let project = MonarchTimesheetDiffField("project")
    /// The activity.
    public static let activity = MonarchTimesheetDiffField("activity")
    /// The task.
    public static let task = MonarchTimesheetDiffField("task")
    /// Whether the time is billable.
    public static let billable = MonarchTimesheetDiffField("billable")
    /// The note.
    public static let note = MonarchTimesheetDiffField("note")
    /// The start time, for products that record it.
    public static let start = MonarchTimesheetDiffField("start")
    /// The end time, for products that record it.
    public static let end = MonarchTimesheetDiffField("end")
}
