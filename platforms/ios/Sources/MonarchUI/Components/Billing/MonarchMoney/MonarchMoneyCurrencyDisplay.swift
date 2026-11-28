/// How the currency of an amount is written.
public enum MonarchMoneyCurrencyDisplay: Sendable, Hashable, CaseIterable {
    /// The locale's symbol, such as US$ or $.
    case symbol
    /// The shortest symbol, such as $.
    case narrowSymbol
    /// The ISO 4217 code, such as USD.
    case code
    /// The name, such as US dollars.
    case name
}
