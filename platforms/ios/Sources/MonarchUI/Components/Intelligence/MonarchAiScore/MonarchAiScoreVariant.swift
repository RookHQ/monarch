/// How a ``MonarchAiScore`` draws the score.
public enum MonarchAiScoreVariant: Sendable, Hashable, CaseIterable {
    /// A ring with the number in its centre, the default.
    case ring
    /// A horizontal bar with the number above it and a mark at each band.
    case bar
}
