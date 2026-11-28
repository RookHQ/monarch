/// Which part of a day an entry of a ``MonarchTeamCalendar`` covers.
@_spi(Experimental) public enum MonarchTeamCalendarPortion: Sendable, Hashable, CaseIterable {
    /// The whole day.
    case full
    /// The first half of the day, drawn as a half bar at the leading side of the day.
    case firstHalf
    /// The second half of the day, drawn as a half bar at the trailing side of the day.
    case secondHalf
}
