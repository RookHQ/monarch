/// Why a level of ``MonarchEscalationPolicyEditor`` cannot be saved.
public enum MonarchEscalationLevelProblem: Sendable, Hashable, CaseIterable {
    /// The level notifies nobody.
    case targets
    /// With cumulative timing, the level is not later than the one before.
    case order
}
