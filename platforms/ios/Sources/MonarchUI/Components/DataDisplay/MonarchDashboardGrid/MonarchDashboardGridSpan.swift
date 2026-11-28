/// How many columns a widget of ``MonarchDashboardGrid`` spans when the grid
/// has more than one column.
public enum MonarchDashboardGridSpan: Sendable, Hashable {
    /// A number of columns, clamped to the columns available.
    case columns(Int)
    /// The whole width of the grid.
    case full

    /// One column. The default.
    public static let one = MonarchDashboardGridSpan.columns(1)
    /// Two columns.
    public static let two = MonarchDashboardGridSpan.columns(2)
}
