/// How much of the content came from AI.
public enum MonarchAiGeneratedBadgeKind: Sendable, Hashable, CaseIterable {
    /// Written by AI.
    case generated
    /// Written by a person with AI help.
    case assisted
    /// Written by AI and since changed by a person.
    case edited
}
