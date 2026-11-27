/// Replacements for the built-in strings of ``MonarchCalendarToolbar``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCalendarToolbarLabels: Sendable {
    /// The Today button. Built in: "Today".
    public var today: String?
    /// The spoken name of the previous button. Receives the unit of the view. Built in:
    /// "Previous week".
    public var previous: (@Sendable (String) -> String)?
    /// The spoken name of the next button. Receives the unit of the view. Built in: "Next week".
    public var next: (@Sendable (String) -> String)?
    /// The spoken name of the view switcher. Built in: "Calendar view".
    public var view: String?
    /// The name of a view in the switcher. Built in: "Day", "Week", "Month", "Agenda".
    public var viewName: (@Sendable (MonarchCalendarToolbarView) -> String)?
    /// The unit a view moves by, used in the previous and next names. Built in: "day", "week",
    /// "month", "period".
    public var unit: (@Sendable (MonarchCalendarToolbarView) -> String)?
    /// The spoken name of the busy indicator. Built in: "Loading".
    public var loading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - today: The Today button.
    ///   - previous: The spoken name of the previous button, from the unit.
    ///   - next: The spoken name of the next button, from the unit.
    ///   - view: The spoken name of the view switcher.
    ///   - viewName: The name of a view in the switcher.
    ///   - unit: The unit a view moves by.
    ///   - loading: The spoken name of the busy indicator.
    public init(
        today: String? = nil,
        previous: (@Sendable (String) -> String)? = nil,
        next: (@Sendable (String) -> String)? = nil,
        view: String? = nil,
        viewName: (@Sendable (MonarchCalendarToolbarView) -> String)? = nil,
        unit: (@Sendable (MonarchCalendarToolbarView) -> String)? = nil,
        loading: String? = nil
    ) {
        self.today = today
        self.previous = previous
        self.next = next
        self.view = view
        self.viewName = viewName
        self.unit = unit
        self.loading = loading
    }
}
