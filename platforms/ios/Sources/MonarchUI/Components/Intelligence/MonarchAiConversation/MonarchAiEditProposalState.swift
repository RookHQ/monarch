/// What happened to a proposed edit.
public enum MonarchAiEditProposalState: String, Sendable, Hashable, Codable, CaseIterable {
    /// Waiting for a person.
    case proposed
    /// Applied to the target.
    case applied
    /// Rejected.
    case rejected
    /// Applied and then undone.
    case reverted
}
