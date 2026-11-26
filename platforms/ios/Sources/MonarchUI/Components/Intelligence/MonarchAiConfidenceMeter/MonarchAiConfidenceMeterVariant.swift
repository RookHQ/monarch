/// How a confidence meter is drawn.
public enum MonarchAiConfidenceMeterVariant: Sendable, Hashable, CaseIterable {
    /// A compact pill, for fields and list rows. The default.
    case badge
    /// A labelled bar with the percentage and the explanation.
    case bar
}
