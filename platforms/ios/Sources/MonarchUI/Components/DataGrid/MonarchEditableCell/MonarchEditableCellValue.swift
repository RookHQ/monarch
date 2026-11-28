import Foundation

/// The value of a ``MonarchEditableCell``.
///
/// Text and select cells hold `.text`, number cells `.number`, date cells
/// `.date` (the first moment of the day in the environment's calendar), and
/// every kind can be `.empty`.
public enum MonarchEditableCellValue: Sendable, Hashable {
    /// No value.
    case empty
    /// A string: the text of a text cell, or the `value` of a select option.
    case text(String)
    /// A number.
    case number(Double)
    /// A day.
    case date(Date)

    /// Whether the value is `.empty` or an empty string.
    public var isEmpty: Bool {
        switch self {
        case .empty: true
        case .text(let text): text.isEmpty
        case .number, .date: false
        }
    }
}
