/// Where a step of a plan is.
public enum MonarchAiPlanStepState: String, Sendable, Hashable, Codable, CaseIterable {
    /// Not started.
    case todo
    /// In progress.
    case active
    /// Finished.
    case done
    /// Left out.
    case skipped
    /// Failed.
    case failed
}
