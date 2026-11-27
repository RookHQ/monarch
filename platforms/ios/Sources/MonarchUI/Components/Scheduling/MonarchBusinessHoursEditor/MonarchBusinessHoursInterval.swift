/// One open interval of a holiday or special-hours date in a ``MonarchBusinessHoursValue``.
///
/// Minutes from midnight, as in ``MonarchWeeklyHoursInterval`` but without a weekday, because
/// the interval belongs to one date: `{ start, end }` as on the web and Android.
public struct MonarchBusinessHoursInterval: Hashable, Sendable {
    /// The opening time, in minutes from midnight.
    public var start: Int
    /// The closing time, in minutes from midnight, exclusive and at most 1440 (the end of the day).
    public var end: Int

    /// An interval.
    ///
    /// - Parameters:
    ///   - start: The opening time, in minutes from midnight.
    ///   - end: The closing time, in minutes from midnight, exclusive and at most 1440.
    public init(start: Int, end: Int) {
        self.start = start
        self.end = end
    }
}
