import Foundation

/// Replacements for the built-in strings of ``MonarchTimesheetDiff``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchTimesheetDiffLabels: Sendable {
    /// The heading when no title is given. Built in: "What changed".
    public var title: String?
    /// The kind of an added entry. Built in: "Added".
    public var added: String?
    /// The kind of a removed entry. Built in: "Removed".
    public var removed: String?
    /// The kind of a changed entry. Receives the names of the fields.
    /// Built in: "Changed time, project".
    public var changed: (@Sendable ([String]) -> String)?
    /// Names of fields in "Changed …". Unknown fields are shown by their
    /// raw name. Built in: "date", "time", "project", "activity", "task",
    /// "billable", "note", "start", "end".
    public var fields: [MonarchTimesheetDiffField: String]
    /// Read before the old line of a changed entry. Built in: "Before".
    public var before: String?
    /// Read before the new line of a changed entry. Built in: "After".
    public var after: String?
    /// Shown for an entry without a project. Built in: "No project".
    public var noProject: String?
    /// Shown for a non-billable entry. Built in: "non-billable".
    public var nonBillable: String?
    /// Shown when nothing changed. Built in: "No changes.".
    public var noChanges: String?
    /// Writes a signed number of minutes. Built in: "+1h 30m", "−45m", "0m".
    public var delta: (@Sendable (Int) -> String)?
    /// The totals line. Receives the total and, when billing applies, the
    /// billable change. Built in: "Total +1h 30m · billable +1h".
    public var totals: (@Sendable (String, String?) -> String)?
    /// Writes an entry's line, shown and read. Built in: "Mon, 12 Jan · 2h ·
    /// Project · Activity · Task".
    public var entry: (@Sendable (MonarchTimesheetDiffEntry) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        added: String? = nil,
        removed: String? = nil,
        changed: (@Sendable ([String]) -> String)? = nil,
        fields: [MonarchTimesheetDiffField: String] = [:],
        before: String? = nil,
        after: String? = nil,
        noProject: String? = nil,
        nonBillable: String? = nil,
        noChanges: String? = nil,
        delta: (@Sendable (Int) -> String)? = nil,
        totals: (@Sendable (String, String?) -> String)? = nil,
        entry: (@Sendable (MonarchTimesheetDiffEntry) -> String)? = nil
    ) {
        self.title = title
        self.added = added
        self.removed = removed
        self.changed = changed
        self.fields = fields
        self.before = before
        self.after = after
        self.noProject = noProject
        self.nonBillable = nonBillable
        self.noChanges = noChanges
        self.delta = delta
        self.totals = totals
        self.entry = entry
    }
}
