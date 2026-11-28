import Foundation

/// Someone who is here now, for ``MonarchPresenceAvatars``.
public struct MonarchPresencePerson: Identifiable, Sendable, Hashable {
    /// A stable id, passed back when the person is tapped.
    public let id: String
    /// The display name, already localised.
    public let name: String
    /// The picture, loaded with `AsyncImage`. Initials without it.
    public let avatarURL: URL?
    /// Picks the colour of the avatar's fallback, so a renamed person keeps their colour.
    public let seed: String
    /// The presence.
    public let status: MonarchPresenceStatus
    /// A custom status message, such as "In a meeting", added to the spoken text.
    public let customStatus: String?
    /// What they are doing here, such as "Editing" or "Viewing slide 4".
    public let activity: String?

    /// A person.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The display name, already localised.
    ///   - avatarURL: The picture, loaded with `AsyncImage`.
    ///   - seed: Picks the fallback colour. The id by default.
    ///   - status: The presence.
    ///   - customStatus: A custom status message.
    ///   - activity: What they are doing here.
    public init(
        id: String,
        name: String,
        avatarURL: URL? = nil,
        seed: String? = nil,
        status: MonarchPresenceStatus = .online,
        customStatus: String? = nil,
        activity: String? = nil
    ) {
        self.id = id
        self.name = name
        self.avatarURL = avatarURL
        self.seed = seed ?? id
        self.status = status
        self.customStatus = customStatus
        self.activity = activity
    }
}
