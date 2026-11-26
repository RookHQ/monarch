/// The agent that answered, from the stream's `agent` event.
public struct MonarchAiAgentRef: Sendable, Hashable, Codable {
    /// A stable id, for example `dossier-assistant`.
    public var id: String?
    /// The display name.
    public var name: String
    /// A short codename shown in compact places.
    public var codename: String?
    /// What the agent does, for example "Writing assistant".
    public var role: String?
    /// The URL of the agent's avatar image.
    public var avatar: String?

    /// An agent.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The display name.
    ///   - codename: A short codename shown in compact places.
    ///   - role: What the agent does.
    ///   - avatar: The URL of the agent's avatar image.
    public init(id: String? = nil, name: String, codename: String? = nil, role: String? = nil, avatar: String? = nil) {
        self.id = id
        self.name = name
        self.codename = codename
        self.role = role
        self.avatar = avatar
    }
}
