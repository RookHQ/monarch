/// How a gauge shows its value.
public enum MonarchGaugeIndicator: Sendable, Hashable, CaseIterable {
    /// The track is filled up to the value. Bands are thin arcs outside it. The default.
    case arc
    /// The bands fill the track and a needle points at the value.
    case needle
}
