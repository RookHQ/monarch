/// What marks each event of a ``MonarchActivityTimeline`` on its line.
public enum MonarchActivityTimelineMarker: Sendable, Hashable, CaseIterable {
    /// A dot in the event's tone. The default.
    case dot
    /// The actor's avatar; events without an actor keep the dot.
    case avatar
}
