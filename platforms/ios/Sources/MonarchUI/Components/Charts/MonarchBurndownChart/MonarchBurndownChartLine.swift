/// A line of a ``MonarchBurndownChart``, for showing and hiding it.
public enum MonarchBurndownChartLine: String, Sendable, Hashable, CaseIterable {
    /// The work left at the end of each day.
    case remaining
    /// The straight line from the starting work to zero on the last day.
    case ideal
    /// The total committed work, a step line.
    case scope
}
