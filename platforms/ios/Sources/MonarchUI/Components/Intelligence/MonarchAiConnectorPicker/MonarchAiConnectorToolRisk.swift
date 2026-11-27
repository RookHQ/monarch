/// What a ``MonarchAiConnectorTool`` can do to the person's data.
public enum MonarchAiConnectorToolRisk: String, Sendable, Hashable, CaseIterable {
    /// It only reads data.
    case read
    /// It changes data.
    case write
    /// It deletes data or does something that cannot be undone.
    case destructive
}
