/// Where an insight in ``MonarchAiInsightCallout`` came from. Always shown,
/// so people know whether a model or a fixed rule raised it.
public enum MonarchAiInsightOrigin: Sendable, Hashable, CaseIterable {
    /// Produced by a model. Labelled "AI insight" with the assistant mark and colours.
    case ai
    /// Raised by a fixed rule written by people. Labelled "Rule-based" in neutral colours.
    case rule
}
