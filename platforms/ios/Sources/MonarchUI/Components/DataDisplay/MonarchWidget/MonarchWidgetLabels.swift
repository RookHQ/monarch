/// Replacements for the built-in strings of ``MonarchWidget``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchWidgetLabels: Sendable {
    /// The spoken name of the menu button. Receives the title. Built in:
    /// "More actions for Team load".
    public var menu: (@Sendable (String) -> String)?
    /// The visible title of the view-all button. Built in: "View all".
    public var viewAll: String?
    /// The spoken name of the view-all button; start it with the visible
    /// text. Receives the title. Built in: "View all: Team load".
    public var viewAllLabel: (@Sendable (String) -> String)?
    /// The spoken name of the loading placeholder. Receives the title.
    /// Built in: "Loading Team load".
    public var loading: (@Sendable (String) -> String)?
    /// The failure message when the state has none. Built in: "This section
    /// could not be loaded."
    public var error: String?
    /// The empty title when the state has none. Built in: "Nothing to show yet".
    public var empty: String?
    /// The last-updated line. Receives the relative time, such as "5
    /// minutes ago". Built in: "Updated 5 minutes ago".
    public var updated: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        menu: (@Sendable (String) -> String)? = nil,
        viewAll: String? = nil,
        viewAllLabel: (@Sendable (String) -> String)? = nil,
        loading: (@Sendable (String) -> String)? = nil,
        error: String? = nil,
        empty: String? = nil,
        updated: (@Sendable (String) -> String)? = nil
    ) {
        self.menu = menu
        self.viewAll = viewAll
        self.viewAllLabel = viewAllLabel
        self.loading = loading
        self.error = error
        self.empty = empty
        self.updated = updated
    }
}
