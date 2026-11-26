import Foundation

/// Who receives the work in a ``MonarchAiHandoff``: another agent, or a person.
@_spi(Experimental) public enum MonarchAiHandoffTarget: Sendable, Hashable {
    /// Another AI agent. Shown with ``MonarchAiAgentAvatar`` and named with "AI agent".
    case agent(MonarchAiAgentRef)
    /// A person. Shown with a round ``MonarchAvatar``.
    ///
    /// - Parameters:
    ///   - name: The person's name, already localised.
    ///   - avatarURL: The person's picture. Initials without it.
    case person(name: String, avatarURL: URL? = nil)

    /// The display name of the target.
    public var name: String {
        switch self {
        case .agent(let agent): agent.name
        case .person(let name, _): name
        }
    }

    /// Whether the work goes to a person rather than an agent.
    public var isPerson: Bool {
        if case .person = self { return true }
        return false
    }
}
