import Foundation

/// An amount in one currency in a ``MonarchSavingsPanel``.
@_spi(Experimental) public struct MonarchSavingsAmount: Sendable, Hashable {
    /// The amount, in major units.
    public let amount: Decimal
    /// The ISO 4217 code.
    public let currencyCode: String

    /// An amount.
    ///
    /// - Parameters:
    ///   - amount: The amount, in major units.
    ///   - currencyCode: The ISO 4217 code.
    public init(_ amount: Decimal, currencyCode: String) {
        self.amount = amount
        self.currencyCode = currencyCode
    }
}
