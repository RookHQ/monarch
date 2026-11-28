/// How much of a stage journey is shown.
public enum MonarchStageJourneyVariant: Sendable, Hashable, CaseIterable {
    /// The stage strip, a card for the current stage and the moves. The
    /// default.
    case full
    /// The stage strip only, for a row or a header.
    case strip
}
