import Foundation

/// One suggestion of a ``MonarchMentionPicker``. The product's search returns these.
public struct MonarchMentionItem: Identifiable, Sendable, Hashable {
    /// A stable id, such as a user or channel id.
    public let id: String
    /// The display name, already localised.
    public let label: String
    /// A person or a channel.
    public let kind: MonarchMentionKind
    /// A second line: an email address, a role, a member count.
    public let detail: String?
    /// The person's picture, loaded with `AsyncImage`. Initials without it.
    public let avatarURL: URL?
    /// Shown but cannot be chosen, for example someone without access to the conversation.
    public let isDisabled: Bool

    /// A suggestion.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The display name, already localised.
    ///   - kind: A person or a channel.
    ///   - detail: A second line.
    ///   - avatarURL: The person's picture.
    ///   - isDisabled: Shown but cannot be chosen.
    public init(
        id: String,
        label: String,
        kind: MonarchMentionKind = .person,
        detail: String? = nil,
        avatarURL: URL? = nil,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.detail = detail
        self.avatarURL = avatarURL
        self.isDisabled = isDisabled
    }

    /// The token stored for this mention, the same format as the web:
    /// `@[Ana Lopez](u_123)`. Brackets and parentheses are dropped from the
    /// label so the token always parses back.
    ///
    /// - Parameter trigger: The character that started the mention.
    /// - Returns: The token.
    public func token(trigger: Character = "@") -> String {
        let cleaned = label.filter { !"[]()".contains($0) }
        return "\(trigger)[\(cleaned)](\(id))"
    }
}
