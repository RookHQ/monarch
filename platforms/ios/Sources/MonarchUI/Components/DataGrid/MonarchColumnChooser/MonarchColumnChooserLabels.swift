/// Replacements for the built-in strings of ``MonarchColumnChooser``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchColumnChooserLabels: Sendable {
    /// The title. Built in: "Columns".
    public var title: String?
    /// The count under the title, from the shown and total counts. Built in: "5 of 8 shown".
    public var shownCount: (@Sendable (Int, Int) -> String)?
    /// The prompt of the search field. Built in: "Find a column".
    public var search: String?
    /// Shown when the search matches nothing. Built in: "No columns match."
    public var noMatches: String?
    /// The accessibility action that moves a column up, from its name. Built in: "Move Status up".
    public var moveUp: (@Sendable (String) -> String)?
    /// The accessibility action that moves a column down. Built in: "Move Status down".
    public var moveDown: (@Sendable (String) -> String)?
    /// Announced after a move, from the name, the new position and the
    /// count, positions from 1. Built in: "Status moved to position 3 of 8."
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// The note under a column that cannot be hidden. Built in: "Always shown".
    public var required: String?
    /// The button that shows every column. Built in: "Show all".
    public var showAll: String?
    /// The button that runs `onReset`. Built in: "Reset".
    public var reset: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        shownCount: (@Sendable (Int, Int) -> String)? = nil,
        search: String? = nil,
        noMatches: String? = nil,
        moveUp: (@Sendable (String) -> String)? = nil,
        moveDown: (@Sendable (String) -> String)? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        required: String? = nil,
        showAll: String? = nil,
        reset: String? = nil
    ) {
        self.title = title
        self.shownCount = shownCount
        self.search = search
        self.noMatches = noMatches
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.moved = moved
        self.required = required
        self.showAll = showAll
        self.reset = reset
    }
}
