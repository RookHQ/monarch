/// Opening or working hours on one weekday, for a ``MonarchTimeGrid``. Time
/// outside them is shaded.
///
/// ```swift
/// // Monday to Friday, 09:00 to 17:00.
/// (1...5).map { MonarchTimeGridWorkingHours(weekday: $0, start: 9 * 60, end: 17 * 60) }
/// ```
public struct MonarchTimeGridWorkingHours: Hashable, Sendable {
    /// The ISO weekday: 1 is Monday, 7 is Sunday.
    public let weekday: Int
    /// Wall-clock minutes from midnight when the hours start.
    public let start: Int
    /// Wall-clock minutes from midnight when the hours end (exclusive).
    public let end: Int

    /// One interval of working hours.
    ///
    /// - Parameters:
    ///   - weekday: The ISO weekday, 1 (Monday) to 7 (Sunday).
    ///   - start: Minutes from midnight when the hours start.
    ///   - end: Minutes from midnight when the hours end. Give several
    ///     intervals for a day with a break.
    public init(weekday: Int, start: Int, end: Int) {
        self.weekday = weekday
        self.start = start
        self.end = end
    }
}
