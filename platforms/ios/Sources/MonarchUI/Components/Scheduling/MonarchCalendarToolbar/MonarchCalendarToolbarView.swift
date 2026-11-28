/// A view of a calendar, offered by the switcher of ``MonarchCalendarToolbar``.
public enum MonarchCalendarToolbarView: String, Sendable, Hashable, CaseIterable {
    /// One day. Previous and next move by a day.
    case day
    /// One week. Previous and next move by a week.
    case week
    /// One month. Previous and next move by a month.
    case month
    /// A list of events. Previous and next move by a period the app decides.
    case agenda
}
