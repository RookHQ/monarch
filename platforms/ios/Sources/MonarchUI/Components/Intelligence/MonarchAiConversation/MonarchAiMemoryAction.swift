/// What happened to a memory.
public enum MonarchAiMemoryAction: String, Sendable, Hashable, Codable, CaseIterable {
    /// The assistant used a memory it had.
    case read
    /// The assistant saved or updated a memory.
    case write
    /// The assistant was told to forget a memory.
    case forget
}
