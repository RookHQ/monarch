/// Why one holiday or special-hours date of a ``MonarchBusinessHoursValue`` is invalid.
public enum MonarchBusinessHoursExceptionError: String, Sendable, Hashable, CaseIterable {
    /// No date is chosen.
    case date
    /// The name is empty or only spaces.
    case name
    /// An earlier row has the same day.
    case duplicate
    /// A special-hours interval closes before it opens or falls outside the day.
    case hours
}
