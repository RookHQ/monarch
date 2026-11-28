/// What a ``MonarchMentionItem`` points at. Suggestions are grouped by kind.
public enum MonarchMentionKind: Sendable, Hashable, CaseIterable {
    /// A person, shown with an avatar. The default.
    case person
    /// A channel, shown with a number sign.
    case channel
}
