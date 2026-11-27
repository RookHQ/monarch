/// How a ``MonarchTimesheetGrid`` lays out the period.
@_spi(Experimental) public enum MonarchTimesheetGridLayout: Sendable, Hashable, CaseIterable {
    /// A grid of rows against days at regular width, and one day per page at
    /// compact width and at the accessibility text sizes. The default.
    case automatic
    /// Always a grid of rows against days, scrolling sideways when it does
    /// not fit.
    case grid
    /// Always one day per page, with buttons to move between days.
    case day
}
