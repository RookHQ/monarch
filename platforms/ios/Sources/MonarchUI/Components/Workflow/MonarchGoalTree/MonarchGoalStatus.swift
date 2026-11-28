/// How a goal is doing, for ``MonarchGoal``.
@_spi(Experimental) public enum MonarchGoalStatus: Sendable, Hashable, CaseIterable {
    /// Work has not started.
    case notStarted
    /// Expected to be met.
    case onTrack
    /// Might be missed without a change.
    case atRisk
    /// Expected to be missed.
    case offTrack
    /// Met or closed.
    case done
}
