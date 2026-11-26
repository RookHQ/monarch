/// How a ``MonarchShiftRoster`` lays out people and days.
@_spi(Experimental) public enum MonarchShiftRosterLayout: Sendable, Hashable, CaseIterable {
    /// The grid when every day column fits the width and the text is not
    /// enlarged past the xxLarge size, otherwise ``byDay``.
    case automatic
    /// People down the side and days across, scrolled sideways when it does
    /// not fit. For an iPad or a wide window.
    case grid
    /// One day at a time, picked with tabs, with a row per person. The
    /// iPhone layout.
    case byDay
    /// One person at a time, picked with tabs, with a row per day.
    case byPerson
}
