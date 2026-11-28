/// One available interval of a weekly schedule, the value of a ``MonarchWeeklyHoursEditor``.
///
/// A list of these is the whole value, a plain shape that goes to a server unchanged, as on the
/// web and Android: `{ day, start, end }` with ISO weekdays and minutes from midnight.
public struct MonarchWeeklyHoursInterval: Hashable, Sendable {
    /// The ISO weekday: 1 is Monday and 7 is Sunday.
    public var day: Int
    /// The start, in minutes from midnight.
    public var start: Int
    /// The end, in minutes from midnight, exclusive and at most 1440 (the end of the day).
    public var end: Int

    /// An interval.
    ///
    /// - Parameters:
    ///   - day: The ISO weekday: 1 is Monday and 7 is Sunday.
    ///   - start: The start, in minutes from midnight.
    ///   - end: The end, in minutes from midnight, exclusive and at most 1440.
    public init(day: Int, start: Int, end: Int) {
        self.day = day
        self.start = start
        self.end = end
    }

    /// Checks every day of a value: times fall within the day (``MonarchWeeklyHoursError/range``),
    /// each interval ends after it starts (``MonarchWeeklyHoursError/order``) and no two
    /// intervals overlap (``MonarchWeeklyHoursError/overlap``).
    ///
    /// Returns the first problem per ISO weekday; an empty dictionary means the value is valid.
    /// The editor shows these under each day; the app decides whether to block saving.
    public static func validate(_ value: [MonarchWeeklyHoursInterval]) -> [Int: MonarchWeeklyHoursError] {
        var out: [Int: MonarchWeeklyHoursError] = [:]
        for day in 1...7 {
            if let error = dayError(value.filter { $0.day == day }) { out[day] = error }
        }
        return out
    }

    private static func dayError(_ ranges: [MonarchWeeklyHoursInterval]) -> MonarchWeeklyHoursError? {
        if ranges.contains(where: { $0.start < 0 || $0.end > 1440 }) { return .range }
        if ranges.contains(where: { $0.end <= $0.start }) { return .order }
        let sorted = ranges.sorted { $0.start < $1.start }
        for (first, second) in zip(sorted, sorted.dropFirst()) where second.start < first.end {
            return .overlap
        }
        return nil
    }
}
