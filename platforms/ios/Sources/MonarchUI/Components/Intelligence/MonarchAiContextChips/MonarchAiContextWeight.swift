/// How much of the context one item takes, from its share of the total or of a budget.
public enum MonarchAiContextWeight: Sendable, Hashable, CaseIterable {
    /// Less than a fifth.
    case light
    /// From a fifth to a half.
    case medium
    /// Half or more.
    case heavy

    /// The weight of `tokens` against `reference`: heavy at half the
    /// reference or more, medium from a fifth, otherwise light. A reference
    /// or count of zero or less is light.
    public init(tokens: Int, reference: Int) {
        guard reference > 0, tokens > 0 else {
            self = .light
            return
        }
        let share = Double(tokens) / Double(reference)
        self = share >= 0.5 ? .heavy : share >= 0.2 ? .medium : .light
    }
}
