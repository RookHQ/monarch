/// How a ``MonarchCondition`` compares a field's value.
public enum MonarchConditionOperator: String, Hashable, Sendable, CaseIterable {
    /// The value is the rule's value. Text ignores case; dates compare by day.
    case equals
    /// The value is not the rule's value.
    case notEquals
    /// The text contains the rule's text, ignoring case.
    case contains
    /// The text does not contain the rule's text, ignoring case.
    case notContains
    /// The number is greater than the rule's number.
    case greaterThan
    /// The number is at least the rule's number.
    case atLeast
    /// The number is less than the rule's number.
    case lessThan
    /// The number is at most the rule's number.
    case atMost
    /// The date is before the rule's day.
    case before
    /// The date is after the rule's day.
    case after
    /// The value is one of the rule's values. Takes several values. Added after the first fourteen operators.
    case anyOf
    /// The value is none of the rule's values. Takes several values. Added after the first fourteen operators.
    case noneOf
    /// The value is yes. Takes no value.
    case isTrue
    /// The value is no. Takes no value.
    case isFalse
    /// The field has no value. Takes no value.
    case isEmpty
    /// The field has a value. Takes no value.
    case isNotEmpty

    /// Whether the operator needs a value: `isTrue`, `isFalse`, `isEmpty` and `isNotEmpty` do not.
    public var takesValue: Bool {
        switch self {
        case .isTrue, .isFalse, .isEmpty, .isNotEmpty: false
        default: true
        }
    }
}
