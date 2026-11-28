/// How two items of a ``MonarchGanttTimeline`` depend on each other.
@_spi(Experimental) public enum MonarchGanttTimelineDependencyKind: String, Sendable, Hashable, CaseIterable {
    /// The second item starts after the first one finishes.
    case finishToStart
    /// The second item starts when the first one starts.
    case startToStart
    /// The second item finishes when the first one finishes.
    case finishToFinish
}
