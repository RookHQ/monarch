import Foundation

/// One person or agent in a ``MonarchAiSharedSession``.
@_spi(Experimental) public struct MonarchAiSharedSessionParticipant: Identifiable, Sendable, Hashable {
    /// A stable id. Answers, requests and follows refer to it.
    public var id: String
    /// The display name.
    public var name: String
    /// The picture, loaded with `AsyncImage`. Initials without it.
    public var avatarURL: URL?
    /// A person or an AI agent.
    public var kind: MonarchAiSharedSessionKind
    /// What a person may do. Ignored for agents.
    public var role: MonarchAiSharedSessionRole
    /// How present they are.
    public var presence: MonarchAiSharedSessionPresence
    /// What they are doing right now, if anything.
    public var activity: MonarchAiSharedSessionActivity?
    /// Marks the person looking at this screen. Their role decides what they can do here.
    public var isYou: Bool

    /// A participant.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The display name, already localised.
    ///   - avatarURL: The picture.
    ///   - kind: A person or an AI agent.
    ///   - role: What a person may do.
    ///   - presence: How present they are.
    ///   - activity: What they are doing right now.
    ///   - isYou: Marks the person looking at this screen.
    public init(
        id: String,
        name: String,
        avatarURL: URL? = nil,
        kind: MonarchAiSharedSessionKind = .person,
        role: MonarchAiSharedSessionRole = .editor,
        presence: MonarchAiSharedSessionPresence = .active,
        activity: MonarchAiSharedSessionActivity? = nil,
        isYou: Bool = false
    ) {
        self.id = id
        self.name = name
        self.avatarURL = avatarURL
        self.kind = kind
        self.role = role
        self.presence = presence
        self.activity = activity
        self.isYou = isYou
    }
}
