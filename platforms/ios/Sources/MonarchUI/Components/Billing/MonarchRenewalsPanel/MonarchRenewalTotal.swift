import Foundation

/// The total of the renewals in one currency, shown above a
/// ``MonarchRenewalsPanel``. Amounts are never converted or added across currencies.
@_spi(Experimental) public struct MonarchRenewalTotal: Identifiable, Sendable, Hashable {
    /// The ISO 4217 code.
    public let currencyCode: String
    /// The sum of the priced renewals in this currency, in major units.
    public let amount: Decimal
    /// How many renewals are in the total.
    public let count: Int
    /// The part of the total that is estimated rather than firm.
    public let estimated: Decimal?

    /// The currency code.
    public var id: String { currencyCode }

    /// A total in one currency.
    ///
    /// - Parameters:
    ///   - currencyCode: The ISO 4217 code.
    ///   - amount: The sum, in major units.
    ///   - count: How many renewals are in the total.
    ///   - estimated: The part of the total that is estimated.
    public init(currencyCode: String, amount: Decimal, count: Int, estimated: Decimal? = nil) {
        self.currencyCode = currencyCode
        self.amount = amount
        self.count = count
        self.estimated = estimated
    }
}
