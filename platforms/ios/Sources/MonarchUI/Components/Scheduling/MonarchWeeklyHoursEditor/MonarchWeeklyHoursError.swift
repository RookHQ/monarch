/// Why the intervals of one day of a ``MonarchWeeklyHoursEditor`` value are invalid.
public enum MonarchWeeklyHoursError: String, Sendable, Hashable, CaseIterable {
    /// A time falls outside the day: a start before 0 or an end after 1440 minutes.
    case range
    /// An interval does not end after it starts.
    case order
    /// Two intervals of the day overlap.
    case overlap
}
