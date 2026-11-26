/// Replacements for the built-in strings of ``MonarchKanbanBoard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchKanbanBoardLabels: Sendable {
    /// The spoken name of a card's Move menu, from the card. Built in: "Move Fix login to…".
    public var moveMenu: (@Sendable (_ card: String) -> String)?
    /// The heading of the columns in the Move menu. Built in: "Move to".
    public var moveToSection: String?
    /// The VoiceOver action to another column, from its title. Built in: "Move to Doing".
    public var moveTo: (@Sendable (_ column: String) -> String)?
    /// Built in: "Move up".
    public var moveUp: String?
    /// Built in: "Move down".
    public var moveDown: String?
    /// The count in a column header, from the count and the limit. Built in: "2" or "2 / 3".
    public var countText: (@Sendable (_ count: Int, _ limit: Int?) -> String)?
    /// The spoken count, from the count and the limit. Built in: "2 cards, limit 2, at the limit".
    public var count: (@Sendable (_ count: Int, _ limit: Int?) -> String)?
    /// Shown in a column with no cards and no text of its own. Built in: "No cards".
    public var empty: String?
    /// The spoken name of the row of column tabs. Built in: "Columns".
    public var columns: String?
    /// Announced after a move from the menu or a VoiceOver action, from the card, the column, the
    /// position and the total. Built in: "Fix login: Doing, position 1 of 2."
    public var moved: (@Sendable (_ card: String, _ column: String, _ position: Int, _ total: Int) -> String)?
    /// Announced after a drop. Built in: "Fix login dropped in Doing, position 1 of 2."
    public var dropped: (@Sendable (_ card: String, _ column: String, _ position: Int, _ total: Int) -> String)?
    /// Announced when a column refuses a dropped card. Built in: "Fix login cannot move to Done."
    public var notAllowed: (@Sendable (_ card: String, _ column: String) -> String)?
    /// Announced when `onMove` throws and the card goes back. Built in:
    /// "Could not move Fix login. It is back in To do."
    public var failed: (@Sendable (_ card: String, _ column: String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        moveMenu: (@Sendable (_ card: String) -> String)? = nil,
        moveToSection: String? = nil,
        moveTo: (@Sendable (_ column: String) -> String)? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        countText: (@Sendable (_ count: Int, _ limit: Int?) -> String)? = nil,
        count: (@Sendable (_ count: Int, _ limit: Int?) -> String)? = nil,
        empty: String? = nil,
        columns: String? = nil,
        moved: (@Sendable (_ card: String, _ column: String, _ position: Int, _ total: Int) -> String)? = nil,
        dropped: (@Sendable (_ card: String, _ column: String, _ position: Int, _ total: Int) -> String)? = nil,
        notAllowed: (@Sendable (_ card: String, _ column: String) -> String)? = nil,
        failed: (@Sendable (_ card: String, _ column: String) -> String)? = nil
    ) {
        self.moveMenu = moveMenu
        self.moveToSection = moveToSection
        self.moveTo = moveTo
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.countText = countText
        self.count = count
        self.empty = empty
        self.columns = columns
        self.moved = moved
        self.dropped = dropped
        self.notAllowed = notAllowed
        self.failed = failed
    }
}
