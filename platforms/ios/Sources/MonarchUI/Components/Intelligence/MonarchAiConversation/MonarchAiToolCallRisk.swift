/// How much harm running a tool can do. `destructive` cannot be undone.
public enum MonarchAiToolCallRisk: String, Sendable, Hashable, Codable, CaseIterable {
    /// Little harm.
    case low
    /// Some harm.
    case medium
    /// Much harm.
    case high
    /// Cannot be undone.
    case destructive
}
