import Foundation

/// A discount used to work out the totals of a ``MonarchInvoiceView`` when
/// the billing system has not given them. It applies to the subtotal before
/// tax and is never more than the subtotal.
public enum MonarchInvoiceDiscount: Sendable, Hashable {
    /// A percentage of the subtotal, such as 10 for 10%. Capped at 100.
    case percent(Decimal)
    /// A fixed amount in major units of the invoice's currency.
    case amount(Decimal)
}
