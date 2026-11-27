/// Why a ``MonarchCondition`` is incomplete.
public enum MonarchConditionProblem: String, Hashable, Sendable, CaseIterable {
    /// No field is chosen, or the stored field no longer exists.
    case field
    /// The operator needs a value and there is none.
    case value
    /// The chosen value is no longer one of the field's choices.
    case option
}
