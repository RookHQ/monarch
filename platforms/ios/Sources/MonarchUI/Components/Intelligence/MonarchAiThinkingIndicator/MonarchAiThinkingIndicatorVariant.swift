/// How ``MonarchAiThinkingIndicator`` shows that the assistant is working.
public enum MonarchAiThinkingIndicatorVariant: Sendable, Hashable, CaseIterable {
    /// A soft band of the assistant colour sweeps across the label. The default.
    case shimmer
    /// A pulsing assistant mark before a still label.
    case mark
}
