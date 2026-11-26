/// Replacements for the built-in strings of ``MonarchAiInlineDiff``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Positions are counted from 1.
@_spi(Experimental) public struct MonarchAiInlineDiffLabels: Sendable {
    /// The spoken name of the whole diff; says the suggestions come from AI. Built in: "AI suggested changes".
    public var label: String?
    /// Read before removed text. Built in: "Deleted:".
    public var deleted: String?
    /// Read before added text. Built in: "Inserted:".
    public var inserted: String?
    /// The position, from the change and the total. Built in: "Change 2 of 5".
    public var position: (@Sendable (Int, Int) -> String)?
    /// Shown when there are no changes. Built in: "No suggested changes".
    public var none: String?
    /// The previous button. Built in: "Previous change".
    public var previous: String?
    /// The next button. Built in: "Next change".
    public var next: String?
    /// The visible title of the accept button. Built in: "Accept".
    public var acceptTitle: String?
    /// The visible title of the reject button. Built in: "Reject".
    public var rejectTitle: String?
    /// The spoken name of the accept button. Built in: "Accept change 2 of 5".
    public var accept: (@Sendable (Int, Int) -> String)?
    /// The spoken name of the reject button. Built in: "Reject change 2 of 5".
    public var reject: (@Sendable (Int, Int) -> String)?
    /// Announced after a decision, with the number of changes left. Built in: "Change accepted. 3 left."
    public var announce: (@Sendable (MonarchAiInlineDiffDecision, Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        deleted: String? = nil,
        inserted: String? = nil,
        position: (@Sendable (Int, Int) -> String)? = nil,
        none: String? = nil,
        previous: String? = nil,
        next: String? = nil,
        acceptTitle: String? = nil,
        rejectTitle: String? = nil,
        accept: (@Sendable (Int, Int) -> String)? = nil,
        reject: (@Sendable (Int, Int) -> String)? = nil,
        announce: (@Sendable (MonarchAiInlineDiffDecision, Int) -> String)? = nil
    ) {
        self.label = label
        self.deleted = deleted
        self.inserted = inserted
        self.position = position
        self.none = none
        self.previous = previous
        self.next = next
        self.acceptTitle = acceptTitle
        self.rejectTitle = rejectTitle
        self.accept = accept
        self.reject = reject
        self.announce = announce
    }
}
