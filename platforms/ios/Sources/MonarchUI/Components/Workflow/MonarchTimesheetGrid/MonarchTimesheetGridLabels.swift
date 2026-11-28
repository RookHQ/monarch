import Foundation

/// Replacements for the built-in strings of ``MonarchTimesheetGrid``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchTimesheetGridLabels: Sendable {
    /// The grid's accessibility name. Built in: "Time per row and day".
    public var caption: String?
    /// The heading of the first column. Built in: "Project".
    public var rowHeader: String?
    /// The heading of the total column. Built in: "Total".
    public var total: String?
    /// The heading of the footer row. Built in: "Day total".
    public var dayTotal: String?
    /// A row's actions menu, from the row's name. Built in: "Actions for Atlas".
    public var rowActions: (@Sendable (String) -> String)?
    /// The name of a cell, from the row's name and the day. Built in:
    /// "Atlas, Mon, Jan 12".
    public var cellLabel: (@Sendable (String, String) -> String)?
    /// Read with a cell of a locked day without a reason. Built in: "Locked".
    public var locked: String?
    /// Read for a cell with no time. Built in: "No time".
    public var noTime: String?
    /// Text that cannot be read as a duration. Built in: "Use a time such as
    /// 1:30, 1.5h or 90m."
    public var invalid: String?
    /// A cell above the daily maximum, from the maximum as text. Built in:
    /// "A day has at most 24h."
    public var tooLong: (@Sendable (String) -> String)?
    /// A save that failed without a description. Built in: "That time could
    /// not be saved."
    public var saveFailed: String?
    /// Under a day total, from the expected duration. Built in: "of 8h".
    public var expected: (@Sendable (String) -> String)?
    /// Read with a day total above the daily maximum. Built in: "Over the
    /// daily limit".
    public var overLimit: String?
    /// Shown when there are no rows. Built in: "No time logged for this period."
    public var empty: String?
    /// Read for today's heading. Built in: "Today".
    public var today: String?
    /// Read for a cell being saved. Built in: "Saving".
    public var saving: String?
    /// The button that shows the previous day. Built in: "Previous day".
    public var previousDay: String?
    /// The button that shows the next day. Built in: "Next day".
    public var nextDay: String?
    /// A row's total on a day page, from the duration. Built in: "Total 7h 30m".
    public var rowTotal: (@Sendable (String) -> String)?
    /// Writes minutes in cells and totals. Built in: "1h 30m".
    public var duration: (@Sendable (Int) -> String)?
    /// Writes a day's heading. Built in: "Mon 12" in the environment's locale.
    public var shortDay: (@Sendable (Date) -> String)?
    /// Writes a day in cell names and on a day page. Built in: "Mon, Jan 12"
    /// in the environment's locale.
    public var longDay: (@Sendable (Date) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        caption: String? = nil,
        rowHeader: String? = nil,
        total: String? = nil,
        dayTotal: String? = nil,
        rowActions: (@Sendable (String) -> String)? = nil,
        cellLabel: (@Sendable (String, String) -> String)? = nil,
        locked: String? = nil,
        noTime: String? = nil,
        invalid: String? = nil,
        tooLong: (@Sendable (String) -> String)? = nil,
        saveFailed: String? = nil,
        expected: (@Sendable (String) -> String)? = nil,
        overLimit: String? = nil,
        empty: String? = nil,
        today: String? = nil,
        saving: String? = nil,
        previousDay: String? = nil,
        nextDay: String? = nil,
        rowTotal: (@Sendable (String) -> String)? = nil,
        duration: (@Sendable (Int) -> String)? = nil,
        shortDay: (@Sendable (Date) -> String)? = nil,
        longDay: (@Sendable (Date) -> String)? = nil
    ) {
        self.caption = caption
        self.rowHeader = rowHeader
        self.total = total
        self.dayTotal = dayTotal
        self.rowActions = rowActions
        self.cellLabel = cellLabel
        self.locked = locked
        self.noTime = noTime
        self.invalid = invalid
        self.tooLong = tooLong
        self.saveFailed = saveFailed
        self.expected = expected
        self.overLimit = overLimit
        self.empty = empty
        self.today = today
        self.saving = saving
        self.previousDay = previousDay
        self.nextDay = nextDay
        self.rowTotal = rowTotal
        self.duration = duration
        self.shortDay = shortDay
        self.longDay = longDay
    }
}
