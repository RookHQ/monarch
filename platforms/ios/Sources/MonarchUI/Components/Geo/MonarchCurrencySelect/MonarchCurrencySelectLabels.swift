/// Replacements for the built-in strings of ``MonarchCurrencySelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCurrencySelectLabels: Sendable {
    /// The heading over the `priority` currencies. Built in: "Suggested".
    public var suggested: String?
    /// The heading over the other currencies when there are suggestions. Built in: "All currencies".
    public var all: String?
    /// The combobox's strings. Its `empty` is built in as "No currencies match".
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
