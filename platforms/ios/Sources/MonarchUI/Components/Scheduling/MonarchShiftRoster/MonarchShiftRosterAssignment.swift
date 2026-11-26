/// What one person does on one day in a ``MonarchShiftRoster``: a shift or a
/// day off. A day that is missing from the roster's value is not assigned.
@_spi(Experimental) public enum MonarchShiftRosterAssignment: Hashable, Sendable {
    /// The person works the shift with this id. An id that is not among the
    /// roster's shifts reads as not assigned and counts no hours.
    case shift(String)
    /// The person has the day off.
    case off
}
