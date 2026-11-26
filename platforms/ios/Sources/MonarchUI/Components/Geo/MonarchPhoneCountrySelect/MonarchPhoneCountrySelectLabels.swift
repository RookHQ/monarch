/// Replacements for the built-in strings of ``MonarchPhoneCountrySelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPhoneCountrySelectLabels: Sendable {
    /// The heading over the `preferred` countries. Built in: "Suggested".
    public var suggested: String?
    /// The heading over the other countries when there are preferred ones. Built in: "All countries".
    public var all: String?
    /// A row's text, from the country name and the calling code. Built in: "Portugal (+351)".
    public var option: (@Sendable (String, String) -> String)?
    /// The combobox's strings. Its `empty` is built in as "No matching country".
    public var combobox: MonarchComboboxLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        suggested: String? = nil,
        all: String? = nil,
        option: (@Sendable (String, String) -> String)? = nil,
        combobox: MonarchComboboxLabels = MonarchComboboxLabels()
    ) {
        self.suggested = suggested
        self.all = all
        self.option = option
        self.combobox = combobox
    }
}
