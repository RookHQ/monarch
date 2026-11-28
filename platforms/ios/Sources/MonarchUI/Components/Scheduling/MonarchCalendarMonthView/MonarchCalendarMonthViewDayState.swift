/// A decoration the app gives a day of ``MonarchCalendarMonthView``: time
/// off, a closure, a holiday.
public struct MonarchCalendarMonthViewDayState: Sendable, Hashable {
    /// Whether the day is shaded as unavailable.
    public var isUnavailable: Bool
    /// A short note beside the date, such as "Holiday". Also read by VoiceOver.
    public var badge: String?

    /// A day decoration.
    ///
    /// - Parameters:
    ///   - isUnavailable: Whether the day is shaded as unavailable.
    ///   - badge: A short note beside the date, already localised.
    public init(isUnavailable: Bool = false, badge: String? = nil) {
        self.isUnavailable = isUnavailable
        self.badge = badge
    }
}
