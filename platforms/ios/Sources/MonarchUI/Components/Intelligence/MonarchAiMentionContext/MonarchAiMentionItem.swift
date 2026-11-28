import Foundation

/// One search result of a ``MonarchAiMentionContext``. The product's search returns these.
public struct MonarchAiMentionItem: Identifiable, Sendable, Hashable {
    /// A stable id. It becomes the context item's id.
    public var id: String
    /// What it points at.
    public var kind: MonarchAiMentionKind
    /// The display name, already localised.
    public var label: String
    /// A second line: a path, a role, a record type.
    public var description: String?
    /// The estimated tokens it would add to the context.
    public var tokens: Int?
    /// A person's picture, loaded with `AsyncImage`. Initials without it.
    public var avatarURL: URL?
    /// Shown but cannot be chosen, for example a document the assistant may not read.
    public var isDisabled: Bool

    /// A mention result.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - kind: What it points at.
    ///   - label: The display name, already localised.
    ///   - description: A second line.
    ///   - tokens: The estimated tokens it would add.
    ///   - avatarURL: A person's picture.
    ///   - isDisabled: Shown but cannot be chosen.
    public init(
        id: String,
        kind: MonarchAiMentionKind,
        label: String,
        description: String? = nil,
        tokens: Int? = nil,
        avatarURL: URL? = nil,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.kind = kind
        self.label = label
        self.description = description
        self.tokens = tokens
        self.avatarURL = avatarURL
        self.isDisabled = isDisabled
    }

    /// The context item for ``MonarchAiContextChips`` once it is chosen.
    /// Documents become `file` items.
    public var contextItem: MonarchAiContextItem {
        MonarchAiContextItem(id: id, kind: kind.contextKind, label: label, tokens: tokens, description: description)
    }
}
