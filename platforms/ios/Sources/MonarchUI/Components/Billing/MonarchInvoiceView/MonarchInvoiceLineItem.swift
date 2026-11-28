import Foundation

/// One billed line of a ``MonarchInvoiceView``.
public struct MonarchInvoiceLineItem: Sendable, Hashable, Identifiable {
    /// A stable id.
    public let id: String
    /// What was billed, such as "Design review".
    public let description: String
    /// A second line under the description, such as a period or SKU.
    public let detail: String?
    /// The quantity.
    public let quantity: Decimal
    /// The price of one unit, in major units of the invoice's currency.
    public let unitPrice: Decimal
    /// The line amount as the billing system calculated it. When `nil` it is
    /// worked out as quantity × unit price, rounded to the currency.
    public let amount: Decimal?

    /// A billed line.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - description: What was billed.
    ///   - detail: A second line under the description.
    ///   - quantity: The quantity.
    ///   - unitPrice: The price of one unit, in major units.
    ///   - amount: The line amount from the billing system; worked out when `nil`.
    public init(
        _ id: String,
        description: String,
        detail: String? = nil,
        quantity: Decimal,
        unitPrice: Decimal,
        amount: Decimal? = nil
    ) {
        self.id = id
        self.description = description
        self.detail = detail
        self.quantity = quantity
        self.unitPrice = unitPrice
        self.amount = amount
    }
}
