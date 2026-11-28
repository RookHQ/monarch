import Foundation

/// An outside app or data source the assistant can be allowed to use, shown
/// by ``MonarchAiConnectorPicker``.
public struct MonarchAiConnector: Identifiable, Sendable, Hashable {
    /// The id.
    public let id: String
    /// The name, such as "Calendar". The search matches it.
    public let name: String
    /// One line on what the connector gives the assistant.
    public let description: String?
    /// The address of its icon. Without it, a letter tile.
    public let iconURL: URL?
    /// Whether the connection works.
    public let status: MonarchAiConnectorStatus
    /// Who it is connected as, such as an email address.
    public let account: String?
    /// Whether the assistant may use it in this conversation. The picker changes it.
    public var isEnabled: Bool
    /// The tools it offers. The picker changes their permissions.
    public var tools: [MonarchAiConnectorTool]

    /// A connector.
    ///
    /// - Parameters:
    ///   - id: The id.
    ///   - name: The name, already localised.
    ///   - description: One line on what it gives the assistant, already localised.
    ///   - iconURL: The address of its icon (`http` or `https`).
    ///   - status: Whether the connection works.
    ///   - account: Who it is connected as.
    ///   - isEnabled: Whether the assistant may use it in this conversation.
    ///   - tools: The tools it offers.
    public init(
        id: String,
        name: String,
        description: String? = nil,
        iconURL: URL? = nil,
        status: MonarchAiConnectorStatus,
        account: String? = nil,
        isEnabled: Bool,
        tools: [MonarchAiConnectorTool] = []
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.iconURL = iconURL
        self.status = status
        self.account = account
        self.isEnabled = isEnabled
        self.tools = tools
    }
}
