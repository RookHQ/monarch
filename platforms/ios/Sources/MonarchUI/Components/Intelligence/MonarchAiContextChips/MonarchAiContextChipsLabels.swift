/// Replacements for the built-in strings of ``MonarchAiContextChips``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiContextChipsLabels: Sendable {
    /// The spoken name of the list. Built in: "Context".
    public var label: String?
    /// The spoken prefix of a kind. Built in: "File", "Selection", "Page", "Record", "App", "Person".
    public var kind: (@Sendable (MonarchAiContextKind) -> String)?
    /// The token count of an item, from the compact number ("1.2K") and the count. Built in: "1.2K tokens".
    public var tokens: (@Sendable (String, Int) -> String)?
    /// The total after the chips, from the compact number and the count. Built in: "≈ 1.2K tokens in context".
    public var total: (@Sendable (String, Int) -> String)?
    /// The spoken name of a remove button, from the item's label. Built in: "Remove contract.pdf from context".
    public var remove: (@Sendable (String) -> String)?
    /// Announced after an item is removed, from its label. Built in: "contract.pdf removed from context".
    public var removed: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: String? = nil,
        kind: (@Sendable (MonarchAiContextKind) -> String)? = nil,
        tokens: (@Sendable (String, Int) -> String)? = nil,
        total: (@Sendable (String, Int) -> String)? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        removed: (@Sendable (String) -> String)? = nil
    ) {
        self.label = label
        self.kind = kind
        self.tokens = tokens
        self.total = total
        self.remove = remove
        self.removed = removed
    }
}
