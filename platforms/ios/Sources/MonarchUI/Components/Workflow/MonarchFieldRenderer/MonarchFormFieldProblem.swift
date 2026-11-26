/// Why an answer is not valid, from ``MonarchFormFieldDefinition/problem(for:)``. A ``MonarchFieldRenderer``
/// shows its message.
public enum MonarchFormFieldProblem: Hashable, Sendable {
    /// A required question has no answer.
    case required
    /// A required consent is not ticked.
    case mustAgree
    /// The text is not an email address.
    case email
    /// The text is not an `http` or `https` web address.
    case url
    /// The answer is not a number.
    case notANumber
    /// The number is below the minimum.
    case min(Double)
    /// The number is above the maximum.
    case max(Double)
    /// The text is shorter than the minimum, ignoring spaces at either end.
    case minLength(Int)
    /// The text is longer than the maximum, ignoring spaces at either end.
    case maxLength(Int)
    /// Fewer options are chosen than the minimum.
    case minSelections(Int)
    /// More options are chosen than the maximum.
    case maxSelections(Int)
}
