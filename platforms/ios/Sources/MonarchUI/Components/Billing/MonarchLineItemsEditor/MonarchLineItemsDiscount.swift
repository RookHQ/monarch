import Foundation

/// A discount on the subtotal of a ``MonarchLineItemsEditor``: a percentage
/// or a fixed amount. It is never more than the subtotal.
@_spi(Experimental) public struct MonarchLineItemsDiscount: Sendable, Hashable {
    /// Whether the value is a percentage or an amount.
    public var kind: MonarchLineItemsDiscountKind
    /// The percentage (5 for 5%) or the amount in major units.
    public var value: Decimal

    /// A discount.
    ///
    /// - Parameters:
    ///   - kind: Whether the value is a percentage or an amount.
    ///   - value: The percentage (5 for 5%) or the amount in major units.
    public init(_ kind: MonarchLineItemsDiscountKind, value: Decimal) {
        self.kind = kind
        self.value = value
    }
}
