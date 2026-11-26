/// Replacements for the built-in strings of ``MonarchAiUndoBanner``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiUndoBannerLabels: Sendable {
    /// Read before the title, so the banner is known to be an AI change. Built in: "AI change:".
    public var aiPrefix: String?
    /// The undo button. Built in: "Undo".
    public var undo: String?
    /// The audit link. Built in: "View in audit log".
    public var audit: String?
    /// The dismiss button's spoken name. Built in: "Dismiss".
    public var dismiss: String?
    /// The disclosure title when there are more than three changes, from their number. Built in: "Show 5 changes".
    public var showChanges: (@Sendable (Int) -> String)?
    /// What VoiceOver reads for a change, from the field, the old and the new value.
    /// Built in: "Status changed from Open to Closed".
    public var change: (@Sendable (String, String, String) -> String)?
    /// Shown and announced after a successful undo. Built in: "Changes undone".
    public var undone: String?
    /// Shown and announced when the undo fails. Built in: "Couldn’t undo. Try again."
    public var undoFailed: String?
    /// Shown and announced when the undo window closes. Built in: "Undo is no longer available."
    public var expired: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        aiPrefix: String? = nil,
        undo: String? = nil,
        audit: String? = nil,
        dismiss: String? = nil,
        showChanges: (@Sendable (Int) -> String)? = nil,
        change: (@Sendable (String, String, String) -> String)? = nil,
        undone: String? = nil,
        undoFailed: String? = nil,
        expired: String? = nil
    ) {
        self.aiPrefix = aiPrefix
        self.undo = undo
        self.audit = audit
        self.dismiss = dismiss
        self.showChanges = showChanges
        self.change = change
        self.undone = undone
        self.undoFailed = undoFailed
        self.expired = expired
    }
}
