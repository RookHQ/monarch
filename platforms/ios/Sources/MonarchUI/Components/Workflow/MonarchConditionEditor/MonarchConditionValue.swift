import Foundation

/// The value of a ``MonarchCondition``, or of a field in a record that a query is tested against.
public enum MonarchConditionValue: Hashable, Sendable {
    /// No value.
    case none
    /// Text.
    case text(String)
    /// A number, or `nil` while none is entered.
    case number(Double?)
    /// A day, or `nil` while none is chosen.
    case date(Date?)
    /// Yes or no.
    case boolean(Bool)
    /// The id of a choice of a select field, or `nil` while none is chosen.
    case option(String?)
    /// The ids of several choices, for the operators `anyOf` and `noneOf`. Added after the first six cases.
    case options([String])

    /// Whether the value is missing: `none`, empty text, or a number, date or choice not set.
    public var isEmpty: Bool {
        switch self {
        case .none: true
        case .text(let text): text.isEmpty
        case .number(let number): number == nil
        case .date(let date): date == nil
        case .boolean: false
        case .option(let id): id == nil
        case .options(let ids): ids.isEmpty
        }
    }
}
