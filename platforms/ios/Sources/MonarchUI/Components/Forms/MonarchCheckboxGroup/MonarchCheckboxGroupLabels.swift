/// Replacements for the built-in strings of ``MonarchCheckboxGroup``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCheckboxGroupLabels: Sendable {
    /// The label of the select-all checkbox. Built in: "Select all".
    public var selectAll: String?
    /// Replacements for the strings of the field around the group: the
    /// spoken "Required" and the spoken form of an error.
    public var field: MonarchFormFieldLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(selectAll: String? = nil, field: MonarchFormFieldLabels = MonarchFormFieldLabels()) {
        self.selectAll = selectAll
        self.field = field
    }
}
