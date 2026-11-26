/// How a ``MonarchLineItemsDiscount`` is given.
@_spi(Experimental) public enum MonarchLineItemsDiscountKind: Sendable, Hashable, CaseIterable {
    /// A percentage of the subtotal.
    case percent
    /// A fixed amount in major units of the document's currency.
    case amount
}
