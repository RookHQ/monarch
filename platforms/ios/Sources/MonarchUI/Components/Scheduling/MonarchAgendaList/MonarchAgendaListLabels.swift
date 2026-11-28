/// Replacements for the built-in strings of ``MonarchAgendaList``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchAgendaListLabels: Sendable {
    /// The marker in today's heading. Built in: "Today".
    public var today: String?
    /// The time of an all-day event. Built in: "All day".
    public var allDay: String?
    /// The count in each day's heading. Receives the number of events. Built in: "1 event",
    /// "3 events".
    public var count: (@Sendable (Int) -> String)?
    /// The title of the empty state. Built in: "Nothing scheduled".
    public var emptyTitle: String?
    /// The spoken time of an event that continues from an earlier day. Receives the end time.
    /// Built in: "until 11:00".
    public var until: (@Sendable (String) -> String)?
    /// The spoken time of an event. Receives the start and end times. Built in: "9:30 to 10:15".
    public var range: (@Sendable (String, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - today: The marker in today's heading.
    ///   - allDay: The time of an all-day event.
    ///   - count: The count in each day's heading, from the number of events.
    ///   - emptyTitle: The title of the empty state.
    ///   - until: The spoken time of an event that continues from an earlier day.
    ///   - range: The spoken time of an event, from its start and end.
    public init(
        today: String? = nil,
        allDay: String? = nil,
        count: (@Sendable (Int) -> String)? = nil,
        emptyTitle: String? = nil,
        until: (@Sendable (String) -> String)? = nil,
        range: (@Sendable (String, String) -> String)? = nil
    ) {
        self.today = today
        self.allDay = allDay
        self.count = count
        self.emptyTitle = emptyTitle
        self.until = until
        self.range = range
    }
}
