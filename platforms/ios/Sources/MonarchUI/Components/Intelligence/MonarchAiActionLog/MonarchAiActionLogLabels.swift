/// Replacements for the built-in strings of ``MonarchAiActionLog``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Closures that describe an entry receive "<agent> <action> <target>".
@_spi(Experimental) public struct MonarchAiActionLogLabels: Sendable {
    /// The spoken name of the list. Built in: "AI activity".
    public var label: String?
    /// The marker after the agent's name. Built in: "AI agent".
    public var actorKind: String?
    /// The undo button. Built in: "Undo".
    public var undo: String?
    /// The spoken name of an entry's undo button. Built in: "Undo: <entry>".
    public var undoLabel: (@Sendable (String) -> String)?
    /// Shown on an entry once undone. Built in: "Undone".
    public var undone: String?
    /// Announced after a successful undo. Built in: "Undone: <entry>".
    public var undoneAnnouncement: (@Sendable (String) -> String)?
    /// Shown and announced when an undo fails. Built in: "Could not undo: <entry>".
    public var undoFailed: (@Sendable (String) -> String)?
    /// The accessibility action that opens a target. Receives the target. Built in: "Open <target>".
    public var openTarget: (@Sendable (String) -> String)?
    /// Shown when there are no entries. Built in: "No AI activity yet."
    public var empty: String?
    /// The button that shows more of a long log. Receives how many it adds. Built in: "Show 3 more actions".
    public var showMore: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        actorKind: String? = nil,
        undo: String? = nil,
        undoLabel: (@Sendable (String) -> String)? = nil,
        undone: String? = nil,
        undoneAnnouncement: (@Sendable (String) -> String)? = nil,
        undoFailed: (@Sendable (String) -> String)? = nil,
        openTarget: (@Sendable (String) -> String)? = nil,
        empty: String? = nil,
        showMore: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.actorKind = actorKind
        self.undo = undo
        self.undoLabel = undoLabel
        self.undone = undone
        self.undoneAnnouncement = undoneAnnouncement
        self.undoFailed = undoFailed
        self.openTarget = openTarget
        self.empty = empty
        self.showMore = showMore
    }
}
