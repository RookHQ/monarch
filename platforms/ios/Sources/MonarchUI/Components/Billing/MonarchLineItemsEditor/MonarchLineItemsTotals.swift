import Foundation

/// The amounts of a ``MonarchLineItemsEditor`` in the currency's minor units
/// (cents, paise), so they add up exactly.
///
/// ```swift
/// let totals = MonarchLineItemsTotals(items, currencyCode: "USD", taxRate: 18)
/// save(total: totals.total)  // 1999 is $19.99
/// ```
///
/// Each row is quantity × unit price rounded to the minor unit, half away
/// from zero. The discount applies to the subtotal and is capped at it; tax
/// applies to what is left. Everything is computed in `Decimal`, never in
/// floating point. These are the totals the editor shows.
@_spi(Experimental) public struct MonarchLineItemsTotals: Sendable, Hashable {
    /// Each row's amount, in row order.
    public let lines: [Int]
    /// The sum of the rows.
    public let subtotal: Int
    /// The discount taken off the subtotal, between zero and the subtotal.
    public let discount: Int
    /// The tax on the discounted subtotal.
    public let tax: Int
    /// The discounted subtotal plus tax.
    public let total: Int

    /// Works out the totals.
    ///
    /// - Parameters:
    ///   - items: The rows. An empty quantity or price counts as zero.
    ///   - currencyCode: The ISO 4217 code, which sets the minor unit: USD 2, JPY 0, KWD 3.
    ///   - taxRate: The tax as a percentage of the discounted subtotal, or `nil` for none.
    ///   - discount: The discount on the subtotal, or `nil` for none.
    public init(
        _ items: [MonarchLineItem],
        currencyCode: String,
        taxRate: Decimal? = nil,
        discount: MonarchLineItemsDiscount? = nil
    ) {
        let digits = MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
        let minor = { (major: Decimal) in MonarchLineItemsEditorTokens.minorUnits(major, digits: digits) }
        let lines = items.map { minor(($0.quantity ?? 0) * ($0.unitPrice ?? 0)) }
        let subtotal = lines.reduce(0, +)
        var off = 0
        if let discount, discount.value > 0 {
            switch discount.kind {
            case .percent:
                off = MonarchLineItemsEditorTokens.rounded(Decimal(subtotal) * min(discount.value, 100) / 100)
            case .amount:
                off = minor(discount.value)
            }
            off = max(0, min(off, max(subtotal, 0)))
        }
        let taxable = subtotal - off
        let tax = taxRate.map { MonarchLineItemsEditorTokens.rounded(Decimal(taxable) * $0 / 100) } ?? 0
        self.lines = lines
        self.subtotal = subtotal
        self.discount = off
        self.tax = tax
        self.total = taxable + tax
    }
}
