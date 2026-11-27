/// The state of one step of ``MonarchDomainProgress``.
public enum MonarchDomainStepState: String, Sendable, Hashable, CaseIterable {
    /// The step is finished.
    case done
    /// The step is in progress.
    case current
    /// The step has not started.
    case todo
    /// A blocking problem belongs to the step.
    case error
}
