/// How a delta indicator is drawn.
public enum MonarchDeltaIndicatorVariant: Sendable, Hashable, CaseIterable {
    /// Inline text with no background. The default.
    case plain
    /// A pill with a tinted background.
    case subtle
}
