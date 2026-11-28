/// How a ``MonarchDomainProblem`` is shown.
public enum MonarchDomainProblemSeverity: Sendable, Hashable, CaseIterable {
    /// A mistake the user must fix. Marks its step as needing attention and
    /// is shown as a warning.
    case blocking
    /// A normal wait, such as "certificate pending". Shown quietly and does
    /// not change a step.
    case info
}
