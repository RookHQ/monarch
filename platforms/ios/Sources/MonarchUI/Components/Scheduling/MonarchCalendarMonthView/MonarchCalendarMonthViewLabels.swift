/// Replacements for the built-in strings of ``MonarchCalendarMonthView``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCalendarMonthViewLabels: Sendable {
    /// The overflow button. Receives the number of hidden events. Built in: "+2 more".
    public var more: (@Sendable (Int) -> String)?
    /// The spoken name of a day. Receives the long date and the number of events.
    /// Built in: "Wednesday, November 4, 2026, 2 events".
    public var day: (@Sendable (String, Int) -> String)?
    /// Added to today's spoken name. Built in: "today".
    public var today: String?
    /// The VoiceOver action that moves an event one day earlier. Built in: "Move one day earlier".
    public var moveDayEarlier: String?
    /// The VoiceOver action that moves an event one day later. Built in: "Move one day later".
    public var moveDayLater: String?
    /// The VoiceOver action that moves an event one week earlier. Built in: "Move one week earlier".
    public var moveWeekEarlier: String?
    /// The VoiceOver action that moves an event one week later. Built in: "Move one week later".
    public var moveWeekLater: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - more: The overflow button, from the number of hidden events.
    ///   - day: The spoken name of a day, from the long date and the number of events.
    ///   - today: Added to today's spoken name.
    ///   - moveDayEarlier: The action that moves an event one day earlier.
    ///   - moveDayLater: The action that moves an event one day later.
    ///   - moveWeekEarlier: The action that moves an event one week earlier.
    ///   - moveWeekLater: The action that moves an event one week later.
    public init(
        more: (@Sendable (Int) -> String)? = nil,
        day: (@Sendable (String, Int) -> String)? = nil,
        today: String? = nil,
        moveDayEarlier: String? = nil,
        moveDayLater: String? = nil,
        moveWeekEarlier: String? = nil,
        moveWeekLater: String? = nil
    ) {
        self.more = more
        self.day = day
        self.today = today
        self.moveDayEarlier = moveDayEarlier
        self.moveDayLater = moveDayLater
        self.moveWeekEarlier = moveWeekEarlier
        self.moveWeekLater = moveWeekLater
    }
}
