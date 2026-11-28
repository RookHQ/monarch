/// Replacements for the built-in strings of ``MonarchAiToolResultCard``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiToolResultCardLabels: Sendable {
    /// The label above the title for a kind. Built in: "Record created", "Email sent",
    /// "Event booked", "Ticket filed" and "Done".
    public var kind: (@Sendable (MonarchAiToolResultKind) -> String)?
    /// The link that opens the result. Built in: "Open".
    public var open: String?
    /// The undo button. Built in: "Undo".
    public var undo: String?
    /// Shown and announced after a successful undo. Built in: "Undone".
    public var undone: String?
    /// Shown and announced when the undo fails. Built in: "Couldn’t undo. Try again."
    public var undoFailed: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        kind: (@Sendable (MonarchAiToolResultKind) -> String)? = nil,
        open: String? = nil,
        undo: String? = nil,
        undone: String? = nil,
        undoFailed: String? = nil
    ) {
        self.kind = kind
        self.open = open
        self.undo = undo
        self.undone = undone
        self.undoFailed = undoFailed
    }
}
