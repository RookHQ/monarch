import Foundation

/// One time entry as it was in a version of a timesheet.
public struct MonarchTimesheetDiffEntry: Sendable, Hashable, Identifiable {
    /// A stable identifier, the same in both versions for an entry that changed.
    public let id: String
    /// The day worked. It is shown in the environment's time zone.
    public let date: Date
    /// The time worked, in minutes.
    public let minutes: Int
    /// The project, client or cost centre.
    public let project: String?
    /// The activity or category.
    public let activity: String?
    /// The task, issue key or a short description.
    public let task: String?
    /// Whether the time is billable, or `nil` when billing does not apply.
    public let isBillable: Bool?
    /// A note in the person's own words.
    public let note: String?

    /// A time entry.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - date: The day worked.
    ///   - minutes: The time worked, in minutes.
    ///   - project: The project, client or cost centre.
    ///   - activity: The activity or category.
    ///   - task: The task, issue key or a short description.
    ///   - isBillable: Whether the time is billable; `nil` when billing does
    ///     not apply.
    ///   - note: A note in the person's own words.
    public init(
        id: String,
        date: Date,
        minutes: Int,
        project: String? = nil,
        activity: String? = nil,
        task: String? = nil,
        isBillable: Bool? = nil,
        note: String? = nil
    ) {
        self.id = id
        self.date = date
        self.minutes = minutes
        self.project = project
        self.activity = activity
        self.task = task
        self.isBillable = isBillable
        self.note = note
    }
}
