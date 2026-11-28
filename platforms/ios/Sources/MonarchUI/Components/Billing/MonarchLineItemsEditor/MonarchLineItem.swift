import Foundation

/// One row of an invoice, quote or proposal, edited by ``MonarchLineItemsEditor``.
///
/// Quantities and prices are `Decimal`, so 3 × 24.50 is exactly 73.50.
@_spi(Experimental) public struct MonarchLineItem: Sendable, Hashable, Identifiable {
    /// The stable key of the row: your saved line id, or a new UUID.
    public var id: String
    /// What is charged for.
    public var description: String
    /// How many units, or `nil` while the field is empty (counted as zero).
    public var quantity: Decimal?
    /// The price of one unit in major units (dollars, rupees), or `nil`
    /// while the field is empty (counted as zero).
    public var unitPrice: Decimal?

    /// A row.
    ///
    /// - Parameters:
    ///   - id: The stable key of the row. Default: a new UUID.
    ///   - description: What is charged for.
    ///   - quantity: How many units. Default: 1.
    ///   - unitPrice: The price of one unit in major units.
    public init(
        id: String = UUID().uuidString,
        description: String = "",
        quantity: Decimal? = 1,
        unitPrice: Decimal? = nil
    ) {
        self.id = id
        self.description = description
        self.quantity = quantity
        self.unitPrice = unitPrice
    }
}
