/// What an AI usage scope counts, for ``MonarchAiUsageMeter``.
public enum MonarchAiUsageUnit: Sendable, Hashable, CaseIterable {
    /// Model tokens.
    case tokens
    /// Billing credits.
    case credits
}
