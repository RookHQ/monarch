/// What an AI model can do, shown as a badge in ``MonarchAiModelPicker``.
public enum MonarchAiModelCapability: String, Sendable, Hashable, Codable, CaseIterable {
    /// Reads images.
    case vision
    /// Calls tools.
    case tools
    /// Reasons step by step before answering.
    case reasoning
    /// Searches the web.
    case web
}
