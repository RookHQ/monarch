/// The spacing of a ``MonarchIncidentTimeline``.
public enum MonarchIncidentTimelineDensity: Sendable, Hashable, CaseIterable {
    /// The standard spacing, for a status page.
    case regular
    /// Tighter spacing, for cards and side panels.
    case compact
}
