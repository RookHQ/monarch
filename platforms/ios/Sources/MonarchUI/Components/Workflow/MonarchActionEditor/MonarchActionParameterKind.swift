/// The input of one ``MonarchActionParameter``.
public enum MonarchActionParameterKind: Hashable, Sendable {
    /// One line of text.
    case text
    /// Several lines of text, such as a message.
    case multilineText
    /// A number, optionally limited.
    case number(minimum: Double?, maximum: Double?)
    /// One of a list of choices, in a menu.
    case select([MonarchComboboxOption])
    /// Any number of a list of choices, in a menu of checkmarks.
    case multiSelect([MonarchComboboxOption])
    /// Free tags, with suggestions.
    case tags(suggestions: [String])
    /// Yes or no, as a checkbox.
    case boolean
}
