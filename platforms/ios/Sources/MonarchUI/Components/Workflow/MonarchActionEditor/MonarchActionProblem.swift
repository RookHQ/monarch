/// Why a ``MonarchRuleAction`` is incomplete, from `MonarchActionEditor.problems(of:types:)`.
///
/// The cases are the same as on Android (`MonarchActionProblem.Missing`, `OutOfRange`, `Invalid`), so a
/// check that runs before saving reads the same on both platforms. Turn one into text with
/// `MonarchActionEditor.message(for:in:labels:)`.
public enum MonarchActionProblem: Hashable, Sendable {
    /// A required parameter that is visible is empty. `parameter` is the parameter's key.
    case missing(parameter: String)
    /// A number is outside the `minimum` and `maximum` of its parameter. `parameter` is the parameter's key.
    case outOfRange(parameter: String)
    /// The type's own `validate` returned a message, already worded by the app.
    case invalid(message: String)
}
