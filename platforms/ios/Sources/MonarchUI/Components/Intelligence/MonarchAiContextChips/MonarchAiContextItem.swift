/// One thing the AI will see with the next prompt, shown by ``MonarchAiContextChips``.
public struct MonarchAiContextItem: Identifiable, Sendable, Hashable {
    /// A stable id, passed to `onRemove`.
    public var id: String
    /// What the item is.
    public var kind: MonarchAiContextKind
    /// A short name: a file name, "Selected text", a record title. Truncated
    /// in the middle on screen, never for VoiceOver.
    public var label: String
    /// The estimated tokens it adds, shown as "1.2K tokens" with a weight mark.
    public var tokens: Int?
    /// Longer text, such as a path or the first words of a selection, read as a hint.
    public var description: String?
    /// Whether the person can remove it. `false` keeps an item that is always
    /// included, such as the current page.
    public var isRemovable: Bool
    /// A group name shown before the label, such as "Page" in "Page Quarterly plan". It also replaces the
    /// kind's spoken name.
    public var group: String?

    /// A context item.
    ///
    /// - Parameters:
    ///   - id: A stable id, passed to `onRemove`.
    ///   - kind: What the item is.
    ///   - label: A short name, already localised.
    ///   - tokens: The estimated tokens it adds.
    ///   - description: Longer text, read as a hint.
    ///   - isRemovable: Whether the person can remove it.
    ///   - group: A group name shown before the label.
    public init(
        id: String,
        kind: MonarchAiContextKind,
        label: String,
        tokens: Int? = nil,
        description: String? = nil,
        isRemovable: Bool = true,
        group: String? = nil
    ) {
        self.id = id
        self.kind = kind
        self.label = label
        self.tokens = tokens
        self.description = description
        self.isRemovable = isRemovable
        self.group = group
    }
}
