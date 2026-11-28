/// The value of one parameter of a ``MonarchRuleAction``.
public enum MonarchActionValue: Hashable, Sendable {
    /// Text, from a text or multi-line text parameter.
    case text(String)
    /// A number, or `nil` while none is entered.
    case number(Double?)
    /// Yes or no.
    case boolean(Bool)
    /// The id of the chosen option of a select, or `nil` for none.
    case option(String?)
    /// The chosen option ids of a multi-select, or the tags of a tags parameter.
    case list([String])

    /// Whether the value is missing: empty text, no number, no option or an empty list. A
    /// boolean is never empty.
    public var isEmpty: Bool {
        switch self {
        case .text(let text): text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .number(let number): number == nil
        case .boolean: false
        case .option(let id): id == nil
        case .list(let values): values.isEmpty
        }
    }
}
