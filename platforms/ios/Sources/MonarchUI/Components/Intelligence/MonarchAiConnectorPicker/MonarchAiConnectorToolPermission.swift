/// What the assistant may do with a ``MonarchAiConnectorTool``.
public enum MonarchAiConnectorToolPermission: String, Sendable, Hashable, CaseIterable {
    /// Run it without asking.
    case allow
    /// Ask the person each time.
    case ask
    /// Never run it.
    case never
}
