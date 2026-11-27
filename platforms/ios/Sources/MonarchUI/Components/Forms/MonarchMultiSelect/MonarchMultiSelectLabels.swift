/// Replacements for the built-in strings of ``MonarchMultiSelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMultiSelectLabels: Sendable {
    /// The field's button while nothing is chosen and there is no prompt. Built in: "Select…".
    public var select: String?
    /// The spoken name of the field's chevron button while something is chosen; it is not drawn. Built in: "Edit selection".
    public var change: String?
    /// The button that closes the list. Built in: "Done".
    public var done: String?
    /// The count under the field when there is a limit. Receives the count and the limit. Built in: "2 of 3 selected".
    public var count: (@Sendable (Int, Int) -> String)?
    /// The list's strings: search, loading, empty, create, more, the spoken
    /// "None", "Required" and error. Its `cancel` and `clear` are not used.
    public var list: MonarchComboboxLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        select: String? = nil,
        change: String? = nil,
        done: String? = nil,
        count: (@Sendable (Int, Int) -> String)? = nil,
        list: MonarchComboboxLabels = MonarchComboboxLabels()
    ) {
        self.select = select
        self.change = change
        self.done = done
        self.count = count
        self.list = list
    }
}
