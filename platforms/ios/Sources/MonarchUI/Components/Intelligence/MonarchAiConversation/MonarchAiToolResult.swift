/// The result of a tool the client ran.
public enum MonarchAiToolResult: Sendable, Hashable {
    /// The tool finished with this output. The call becomes `succeeded`.
    case output(MonarchAiJSON)
    /// The tool failed with this message, and perhaps partial output. The call becomes `failed`.
    case failure(String, output: MonarchAiJSON? = nil)
}
