/// The spacing of a ``MonarchActivityTimeline``.
public enum MonarchActivityTimelineDensity: Sendable, Hashable, CaseIterable {
    /// The default spacing.
    case regular
    /// Tighter spacing, for side panels.
    case compact
}
