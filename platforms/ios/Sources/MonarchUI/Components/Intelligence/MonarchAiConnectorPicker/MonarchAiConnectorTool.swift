/// One action a ``MonarchAiConnector`` offers to the assistant.
public struct MonarchAiConnectorTool: Identifiable, Sendable, Hashable {
    /// The id, unique within its connector.
    public let id: String
    /// The short name, such as "Create event".
    public let name: String
    /// One line on what the tool does.
    public let description: String?
    /// What the tool can do to the person's data.
    public let risk: MonarchAiConnectorToolRisk
    /// What the assistant may do with the tool. The picker changes it.
    public var permission: MonarchAiConnectorToolPermission

    /// A tool.
    ///
    /// - Parameters:
    ///   - id: The id, unique within its connector.
    ///   - name: The short name, already localised.
    ///   - description: One line on what it does, already localised.
    ///   - risk: What it can do to the person's data.
    ///   - permission: What the assistant may do with it.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        risk: MonarchAiConnectorToolRisk,
        permission: MonarchAiConnectorToolPermission
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.risk = risk
        self.permission = permission
    }
}
