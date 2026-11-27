/// Replacements for the built-in strings of ``MonarchCountrySelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCountrySelectLabels: Sendable {
    /// The heading over the `priority` countries. Built in: "Suggested".
    public var suggested: String?
    /// The heading over the other countries when there are suggestions. Built in: "All countries".
    public var all: String?
    /// The combobox's strings. Its `empty` is built in as "No countries match".
    public var combobox: MonarchComboboxLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        suggested: String? = nil,
        all: String? = nil,
        combobox: MonarchComboboxLabels = MonarchComboboxLabels()
    ) {
        self.suggested = suggested
        self.all = all
        self.combobox = combobox
    }
}
