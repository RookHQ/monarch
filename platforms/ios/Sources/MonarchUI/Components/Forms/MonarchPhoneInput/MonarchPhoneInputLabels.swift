/// Replacements for the built-in strings of ``MonarchPhoneInput``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPhoneInputLabels: Sendable {
    /// The spoken name of the country button and the title of the country list. Built in: "Country code".
    public var country: String?
    /// Shown and announced when the number is too short or too long. Built in: "Enter a complete phone number."
    public var invalid: String?
    /// The strings of the country list: headings, row text and search.
    public var countrySelect: MonarchPhoneCountrySelectLabels
    /// The spoken "Required" and error prefix of the field.
    public var field: MonarchFormFieldLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        country: String? = nil,
        invalid: String? = nil,
        countrySelect: MonarchPhoneCountrySelectLabels = MonarchPhoneCountrySelectLabels(),
        field: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.country = country
        self.invalid = invalid
        self.countrySelect = countrySelect
        self.field = field
    }
}
