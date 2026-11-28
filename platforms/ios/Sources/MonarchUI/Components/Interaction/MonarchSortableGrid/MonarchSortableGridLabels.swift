/// Replacements for the built-in strings of ``MonarchSortableGrid``.
///
/// Every property is `nil` (or empty) by default, which keeps the built-in string.
public struct MonarchSortableGridLabels: Sendable {
    /// The spoken name of a tile's drag handle, from the item's name. Built in: "Reorder Design review".
    public var handle: (@Sendable (String) -> String)?
    /// Announced after a move with an accessibility action, from the item's
    /// name, its new position and the number of items, both counted from 1.
    /// Built in: "Design review moved to position 3 of 8."
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// Announced after a drop. Built in: "Design review dropped at position 3 of 8."
    public var dropped: (@Sendable (String, Int, Int) -> String)?
    /// Replaces the names of the handle's move actions ("Move up", "Move forward", …).
    public var actions: [MonarchDragHandleDirection: String]

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        handle: (@Sendable (String) -> String)? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        dropped: (@Sendable (String, Int, Int) -> String)? = nil,
        actions: [MonarchDragHandleDirection: String] = [:]
    ) {
        self.handle = handle
        self.moved = moved
        self.dropped = dropped
        self.actions = actions
    }
}
