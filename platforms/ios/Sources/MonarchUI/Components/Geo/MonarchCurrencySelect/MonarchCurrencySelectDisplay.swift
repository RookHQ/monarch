/// What the field of a ``MonarchCurrencySelect`` shows for the chosen currency.
public enum MonarchCurrencySelectDisplay: Sendable, Hashable, CaseIterable {
    /// The name, such as "Euro". The default.
    case name
    /// The code, such as "EUR", for narrow fields.
    case code
    /// The code and the name, such as "EUR · Euro".
    case codeAndName
}
