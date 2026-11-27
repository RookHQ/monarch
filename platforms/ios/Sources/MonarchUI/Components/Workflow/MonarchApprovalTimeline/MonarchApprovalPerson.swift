import Foundation

/// A person in an approval: who asked, who decided or who can decide.
/// Used by ``MonarchApprovalTimeline`` and `MonarchApprovalQueue`.
public struct MonarchApprovalPerson: Sendable, Hashable {
    /// The display name, already localised.
    public let name: String
    /// The picture, loaded with `AsyncImage`. Initials without it.
    public let avatarURL: URL?
    /// Picks the colour of the avatar's fallback, so a renamed person keeps their colour.
    public let seed: String?

    /// A person.
    ///
    /// - Parameters:
    ///   - name: The display name, already localised.
    ///   - avatarURL: The picture, loaded with `AsyncImage`.
    ///   - seed: Picks the fallback colour. The name by default; pass a stable id.
    public init(_ name: String, avatarURL: URL? = nil, seed: String? = nil) {
        self.name = name
        self.avatarURL = avatarURL
        self.seed = seed
    }
}
