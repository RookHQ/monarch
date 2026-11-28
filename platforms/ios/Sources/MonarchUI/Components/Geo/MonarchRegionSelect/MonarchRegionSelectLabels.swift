/// Replacements for the built-in strings of ``MonarchRegionSelect``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchRegionSelectLabels: Sendable {
    /// The prompt while no country is chosen. Built in: "Choose a country first".
    public var chooseCountryFirst: String?
    /// The prompt of the text field used when the country has no list. Built in: "Type the region".
    public var textEntry: String?
    /// Shown when the regions could not be loaded. Built in: "Couldn’t load the regions."
    public var loadFailed: String?
    /// The combobox's strings. Its `empty` is built in as "No regions match".
    public var combobox: MonarchComboboxLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        chooseCountryFirst: String? = nil,
        textEntry: String? = nil,
        loadFailed: String? = nil,
        combobox: MonarchComboboxLabels = MonarchComboboxLabels()
    ) {
        self.chooseCountryFirst = chooseCountryFirst
        self.textEntry = textEntry
        self.loadFailed = loadFailed
        self.combobox = combobox
    }
}
