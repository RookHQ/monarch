/// The kind of value a ``MonarchConditionField`` holds. It decides the operators and the value input.
public enum MonarchConditionFieldType: Hashable, Sendable {
    /// Free text: is, is not, contains, does not contain, is empty, is not empty.
    case text
    /// A number: is, is not, greater than, at least, less than, at most, is empty, is not empty.
    case number
    /// A day: is, is before, is after, is empty, is not empty.
    case date
    /// Yes or no: is yes, is no.
    case boolean
    /// One of a list of choices: is, is not, is empty, is not empty.
    case select([MonarchComboboxOption])
    /// Several of a list of choices, such as tags: is any of, is none of, is empty, is not empty. Added after
    /// the first five kinds.
    case tags([MonarchComboboxOption])
}
