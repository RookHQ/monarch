/// Replacements for the built-in strings of ``MonarchDashboardGrid``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDashboardGridLabels: Sendable {
    /// The spoken name of the grid. Built in: "Dashboard".
    public var label: String?
    /// The button that moves a widget one place earlier, from its name. Built in: "Move Team load earlier".
    public var moveEarlier: (@Sendable (String) -> String)?
    /// The button that moves a widget one place later. Built in: "Move Team load later".
    public var moveLater: (@Sendable (String) -> String)?
    /// Announced after a move with a button, from the name, the new position
    /// and the count, positions from 1. Built in: "Team load moved to position 2 of 6."
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// Announced after a drop. Built in: "Team load dropped at position 2 of 6."
    public var dropped: (@Sendable (String, Int, Int) -> String)?
    /// The hint on a widget's arrange bar while editing. Built in: "Touch and hold, then drag to move."
    public var dragHint: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        moveEarlier: (@Sendable (String) -> String)? = nil,
        moveLater: (@Sendable (String) -> String)? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        dropped: (@Sendable (String, Int, Int) -> String)? = nil,
        dragHint: String? = nil
    ) {
        self.label = label
        self.moveEarlier = moveEarlier
        self.moveLater = moveLater
        self.moved = moved
        self.dropped = dropped
        self.dragHint = dragHint
    }
}
