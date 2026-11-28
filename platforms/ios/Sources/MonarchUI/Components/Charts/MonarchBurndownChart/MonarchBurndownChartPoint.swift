import Foundation

/// One day (or other step) of a ``MonarchBurndownChart``.
public struct MonarchBurndownChartPoint: Sendable, Hashable {
    /// The day, written with the chart's date format. `nil` when the point has a label.
    public let date: Date?
    /// The day's own text, already localised, such as "Day 3". `nil` when the point has a date.
    public let label: String?
    /// The work left at the end of the day, or `nil` for a day not reached yet.
    public let remaining: Double?
    /// The total committed work that day, or `nil` when scope is not tracked.
    /// A change from the day before is drawn as a scope change.
    public let scope: Double?

    /// A day given by its date.
    ///
    /// - Parameters:
    ///   - date: The day.
    ///   - remaining: The work left at the end of the day; `nil` for a day not reached yet.
    ///   - scope: The total committed work that day.
    public init(date: Date, remaining: Double? = nil, scope: Double? = nil) {
        self.date = date
        self.label = nil
        self.remaining = remaining
        self.scope = scope
    }

    /// A day given by its own text, such as "Day 3" or "Mon".
    ///
    /// - Parameters:
    ///   - label: The day's text, already localised.
    ///   - remaining: The work left at the end of the day; `nil` for a day not reached yet.
    ///   - scope: The total committed work that day.
    public init(label: String, remaining: Double? = nil, scope: Double? = nil) {
        self.date = nil
        self.label = label
        self.remaining = remaining
        self.scope = scope
    }
}
