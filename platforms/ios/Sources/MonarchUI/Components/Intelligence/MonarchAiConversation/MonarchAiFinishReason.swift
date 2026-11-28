/// Why the model stopped writing.
public enum MonarchAiFinishReason: String, Sendable, Hashable, Codable, CaseIterable {
    /// It finished.
    case stop
    /// It hit the output limit, so the answer is cut short.
    case length
    /// A safety filter stopped it.
    case contentFilter = "content-filter"
    /// The model declined.
    case refusal
    /// It is waiting for tool results.
    case toolUse = "tool-use"
    /// Another reason.
    case other
}
