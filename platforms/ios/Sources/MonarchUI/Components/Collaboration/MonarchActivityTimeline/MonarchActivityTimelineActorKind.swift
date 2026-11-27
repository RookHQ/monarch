/// Who an actor of a ``MonarchActivityTimeline`` event is.
public enum MonarchActivityTimelineActorKind: Sendable, Hashable, CaseIterable {
    /// A person. The default.
    case person
    /// A rule or workflow. Gets an "Automated" tag.
    case automation
    /// The system itself. Gets an "Automated" tag.
    case system
}
