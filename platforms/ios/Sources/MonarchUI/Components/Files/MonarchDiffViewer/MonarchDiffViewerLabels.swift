/// Replacements for the built-in strings of ``MonarchDiffViewer``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDiffViewerLabels: Sendable {
    /// The spoken name of the diff. Built in: "Changes from Before to After".
    public var caption: (@Sendable (String, String) -> String)?
    /// The default name of the earlier side. Built in: "Before".
    public var before: String?
    /// The default name of the later side. Built in: "After".
    public var after: String?
    /// The name of the layout switch. Built in: "Layout".
    public var mode: String?
    /// The unified layout. Built in: "Unified".
    public var unified: String?
    /// The side by side layout. Built in: "Side by side".
    public var split: String?
    /// The spoken totals. Built in: "3 added, 1 removed".
    public var stats: (@Sendable (Int, Int) -> String)?
    /// Read before an added line. Built in: "Added".
    public var added: String?
    /// Read before a removed line. Built in: "Removed".
    public var removed: String?
    /// Read for an empty line. Built in: "Blank line".
    public var blankLine: String?
    /// The button that shows a folded run. Built in: "Show 12 unchanged lines".
    public var expand: (@Sendable (Int) -> String)?
    /// The previous change button. Built in: "Previous change".
    public var previous: String?
    /// The next change button. Built in: "Next change".
    public var next: String?
    /// Announced when moving between changes. Built in: "Change 2 of 5".
    public var position: (@Sendable (Int, Int) -> String)?
    /// Shown when the two texts are the same. Built in: "No changes."
    public var noChanges: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        caption: (@Sendable (String, String) -> String)? = nil,
        before: String? = nil,
        after: String? = nil,
        mode: String? = nil,
        unified: String? = nil,
        split: String? = nil,
        stats: (@Sendable (Int, Int) -> String)? = nil,
        added: String? = nil,
        removed: String? = nil,
        blankLine: String? = nil,
        expand: (@Sendable (Int) -> String)? = nil,
        previous: String? = nil,
        next: String? = nil,
        position: (@Sendable (Int, Int) -> String)? = nil,
        noChanges: String? = nil
    ) {
        self.caption = caption
        self.before = before
        self.after = after
        self.mode = mode
        self.unified = unified
        self.split = split
        self.stats = stats
        self.added = added
        self.removed = removed
        self.blankLine = blankLine
        self.expand = expand
        self.previous = previous
        self.next = next
        self.position = position
        self.noChanges = noChanges
    }
}
