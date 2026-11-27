/// Replacements for the built-in strings of ``MonarchAiKnowledgeScope``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiKnowledgeScopeLabels: Sendable {
    /// The spoken name of the tree. Built in: "Knowledge sources".
    public var label: String?
    /// The visible and spoken name of a kind. Built in: "Space", "Document", "App".
    public var kind: (@Sendable (MonarchAiKnowledgeSourceKind) -> String)?
    /// The spoken name of a source, from its label and kind name. Built in: "Handbook, Space".
    public var itemLabel: (@Sendable (String, String) -> String)?
    /// The summary under the tree, from the number of selected sources. Built in: "3 sources selected".
    public var summary: (@Sendable (Int) -> String)?
    /// Shown when there are no sources. Built in: "No sources available".
    public var empty: String?
    /// The spoken name of the button that shows a group's contents, from its label.
    /// Built in: "Show contents of Handbook".
    public var expand: (@Sendable (String) -> String)?
    /// The spoken name of the button that hides a group's contents, from its label.
    /// Built in: "Hide contents of Handbook".
    public var collapse: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        label: String? = nil,
        kind: (@Sendable (MonarchAiKnowledgeSourceKind) -> String)? = nil,
        itemLabel: (@Sendable (String, String) -> String)? = nil,
        summary: (@Sendable (Int) -> String)? = nil,
        empty: String? = nil,
        expand: (@Sendable (String) -> String)? = nil,
        collapse: (@Sendable (String) -> String)? = nil
    ) {
        self.label = label
        self.kind = kind
        self.itemLabel = itemLabel
        self.summary = summary
        self.empty = empty
        self.expand = expand
        self.collapse = collapse
    }
}
