/// What the integer amounts of a ``MonarchAiSpendCap`` count.
@_spi(Experimental) public enum MonarchAiSpendCapUnit: String, Sendable, Hashable, Codable, CaseIterable {
    /// The currency's minor unit: cents, pence, or yen for a currency without one.
    case minor
    /// Millionths of the major unit (12 400 000 is 12.40), for usage priced in fractions of a cent.
    case micros
}
