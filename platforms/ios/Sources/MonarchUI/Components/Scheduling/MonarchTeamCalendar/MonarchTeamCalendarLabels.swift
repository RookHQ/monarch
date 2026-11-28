/// Replacements for the built-in strings of ``MonarchTeamCalendar``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchTeamCalendarLabels: Sendable {
    /// The header of the people column. Built in: "Person".
    public var person: String?
    /// The name of the grid read by VoiceOver, from the range. Built in: "Team calendar, November 2026".
    public var grid: (@Sendable (String) -> String)?
    /// Read with today's date. Built in: "Today".
    public var today: String?
    /// Read with a holiday's day and shown when it is tapped, from its name. Built in: "Holiday: Thanksgiving".
    public var holiday: (@Sendable (String) -> String)?
    /// The days of a bar that spans several days, from the first and the last. Built in:
    /// "Monday, November 2 to Friday, November 6".
    public var range: (@Sendable (String, String) -> String)?
    /// Read for a half day in the first half. Built in: "first half".
    public var firstHalf: String?
    /// Read for a half day in the second half. Built in: "second half".
    public var secondHalf: String?
    /// Read for an entry waiting for approval. Built in: "pending".
    public var pending: String?
    /// The title of the empty state when there are no members. Built in: "Nobody to show".
    public var emptyTitle: String?
    /// The message of the empty state. Built in: "People and their time away appear here."
    public var emptyMessage: String?
    /// The line under the grid that explains shading, half bars and faded bars.
    public var legend: String?
    /// Read while the placeholder is shown. Built in: "Loading team calendar".
    public var loading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - person: The header of the people column.
    ///   - grid: The spoken name of the grid, from the range.
    ///   - today: Read with today's date.
    ///   - holiday: Read with a holiday's day, from its name.
    ///   - range: The days of a bar that spans several days, from the first and the last.
    ///   - firstHalf: Read for a half day in the first half.
    ///   - secondHalf: Read for a half day in the second half.
    ///   - pending: Read for an entry waiting for approval.
    ///   - emptyTitle: The title of the empty state.
    ///   - emptyMessage: The message of the empty state.
    ///   - legend: The line under the grid.
    ///   - loading: Read while the placeholder is shown.
    public init(
        person: String? = nil,
        grid: (@Sendable (String) -> String)? = nil,
        today: String? = nil,
        holiday: (@Sendable (String) -> String)? = nil,
        range: (@Sendable (String, String) -> String)? = nil,
        firstHalf: String? = nil,
        secondHalf: String? = nil,
        pending: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        legend: String? = nil,
        loading: String? = nil
    ) {
        self.person = person
        self.grid = grid
        self.today = today
        self.holiday = holiday
        self.range = range
        self.firstHalf = firstHalf
        self.secondHalf = secondHalf
        self.pending = pending
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.legend = legend
        self.loading = loading
    }
}
