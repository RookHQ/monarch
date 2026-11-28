/// How the days of a ``MonarchCalendarMonthView`` show their events.
public enum MonarchCalendarMonthViewLayout: String, Sendable, Hashable, CaseIterable {
    /// Event chips when the days are wide enough and the text is not an
    /// accessibility size; dots otherwise. The default.
    case automatic
    /// Up to three dots under the date; touching the day selects it, so the
    /// app can list its events below, as on an iPhone.
    case dots
    /// Event chips with a "+N more" button, which can be dragged to another day.
    case events
}
