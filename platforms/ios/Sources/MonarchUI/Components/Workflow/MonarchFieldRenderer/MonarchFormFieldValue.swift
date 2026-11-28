import Foundation

/// An answer to a question of a ``MonarchFieldRenderer``. No answer is `nil`.
public enum MonarchFormFieldValue: Hashable, Sendable {
    /// Text: short and long text, email, phone, web address, the value of a select or radio option, and a
    /// time as `HH:MM` on a 24-hour clock.
    case text(String)
    /// A number, and the value of a rating.
    case number(Double)
    /// A day, as the system date picker gives it.
    case date(Date)
    /// Yes or no, and whether a consent box is ticked.
    case flag(Bool)
    /// The values of the chosen options of a multi-select or checkboxes question, in the order of the options.
    case values([String])
    /// Files, in the order they were added.
    case files([MonarchFormFieldFile])

    /// Whether the answer counts as empty: blank text, not a number, no options and no files.
    public var isEmpty: Bool {
        switch self {
        case .text(let text): text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .number(let number): number.isNaN
        case .date, .flag: false
        case .values(let values): values.isEmpty
        case .files(let files): files.isEmpty
        }
    }
}
