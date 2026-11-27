/// Replacements for the built-in strings of ``MonarchLanguageSelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchLanguageSelectLabels: Sendable {
    /// The heading over the `priority` languages. Built in: "Suggested".
    public var suggested: String?
    /// The heading over the other languages when there are suggestions. Built in: "All languages".
    public var all: String?
    /// The combobox's strings. Its `empty` is built in as "No languages match".
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
