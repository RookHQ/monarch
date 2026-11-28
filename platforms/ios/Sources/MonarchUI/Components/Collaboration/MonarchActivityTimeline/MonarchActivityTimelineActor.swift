import Foundation

/// Who did an event of a ``MonarchActivityTimeline``.
public struct MonarchActivityTimelineActor: Sendable, Hashable {
    /// The name, already localised.
    public let name: String
    /// The avatar image, loaded with `AsyncImage`; initials are shown until it loads.
    public let avatarURL: URL?
    /// A person, an automation or the system.
    public let kind: MonarchActivityTimelineActorKind
    /// Keeps the avatar's colour stable when the name changes, such as a user id.
    public let seed: String?

    /// An actor.
    ///
    /// - Parameters:
    ///   - name: The name, already localised.
    ///   - avatarURL: The avatar image.
    ///   - kind: A person, an automation or the system.
    ///   - seed: Keeps the avatar's colour stable, such as a user id.
    public init(
        _ name: String,
        avatarURL: URL? = nil,
        kind: MonarchActivityTimelineActorKind = .person,
        seed: String? = nil
    ) {
        self.name = name
        self.avatarURL = avatarURL
        self.kind = kind
        self.seed = seed
    }
}
