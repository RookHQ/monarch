import Foundation

/// The totals of a ``MonarchInvoiceView`` as the billing system calculated
/// them, in major units. A value left `nil` is worked out from the line
/// items, the tax rate and the discount, except the amount paid and the
/// amount due, which are shown only when given.
public struct MonarchInvoiceTotals: Sendable, Hashable {
    /// The sum of the lines.
    public var subtotal: Decimal?
    /// The discount, as a positive amount. Shown only when it is not zero.
    public var discount: Decimal?
    /// The tax. Shown when given or when the invoice has a tax rate.
    public var tax: Decimal?
    /// The total.
    public var total: Decimal?
    /// Payments already received. Shown only when given.
    public var amountPaid: Decimal?
    /// What is still owed. Shown only when given.
    public var amountDue: Decimal?

    /// Totals. Every value is optional.
    ///
    /// - Parameters:
    ///   - subtotal: The sum of the lines.
    ///   - discount: The discount, as a positive amount.
    ///   - tax: The tax.
    ///   - total: The total.
    ///   - amountPaid: Payments already received.
    ///   - amountDue: What is still owed.
    public init(
        subtotal: Decimal? = nil,
        discount: Decimal? = nil,
        tax: Decimal? = nil,
        total: Decimal? = nil,
        amountPaid: Decimal? = nil,
        amountDue: Decimal? = nil
    ) {
        self.subtotal = subtotal
        self.discount = discount
        self.tax = tax
        self.total = total
        self.amountPaid = amountPaid
        self.amountDue = amountDue
    }
}
