/// Replacements for the built-in strings of ``MonarchTimeGrid``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchTimeGridLabels: Sendable {
    /// The label of the all-day row and the time of an all-day event. Built in: "All day".
    public var allDay: String?
    /// The spoken name of an empty hour. Receives the day and time. Built in:
    /// "Create event, Wednesday 4 November, 09:00".
    public var createEvent: (@Sendable (String) -> String)?
    /// Added to today's heading for VoiceOver. Built in: "Today".
    public var today: String?
    /// The hint of a day heading that opens the day. Receives the day. Built in:
    /// "Opens Wednesday 4 November".
    public var openDay: (@Sendable (String) -> String)?
    /// The spoken time of an event. Receives the start and end times. Built in: "9:30 to 10:15".
    public var range: (@Sendable (String, String) -> String)?
    /// The VoiceOver action that moves an event earlier by the snap interval. Built in:
    /// "Move earlier".
    public var moveEarlier: String?
    /// The VoiceOver action that moves an event later by the snap interval. Built in:
    /// "Move later".
    public var moveLater: String?
    /// The VoiceOver action that moves an event to the previous day column. Built in:
    /// "Move to previous day".
    public var moveToPreviousDay: String?
    /// The VoiceOver action that moves an event to the next day column. Built in:
    /// "Move to next day".
    public var moveToNextDay: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - allDay: The label of the all-day row.
    ///   - createEvent: The spoken name of an empty hour, from the day and time.
    ///   - today: Added to today's heading for VoiceOver.
    ///   - openDay: The hint of a day heading that opens the day, from the day.
    ///   - range: The spoken time of an event, from its start and end.
    ///   - moveEarlier: The action that moves an event earlier.
    ///   - moveLater: The action that moves an event later.
    ///   - moveToPreviousDay: The action that moves an event to the previous day.
    ///   - moveToNextDay: The action that moves an event to the next day.
    public init(
        allDay: String? = nil,
        createEvent: (@Sendable (String) -> String)? = nil,
        today: String? = nil,
        openDay: (@Sendable (String) -> String)? = nil,
        range: (@Sendable (String, String) -> String)? = nil,
        moveEarlier: String? = nil,
        moveLater: String? = nil,
        moveToPreviousDay: String? = nil,
        moveToNextDay: String? = nil
    ) {
        self.allDay = allDay
        self.createEvent = createEvent
        self.today = today
        self.openDay = openDay
        self.range = range
        self.moveEarlier = moveEarlier
        self.moveLater = moveLater
        self.moveToPreviousDay = moveToPreviousDay
        self.moveToNextDay = moveToNextDay
    }
}
