import Foundation

/// One unused item in a ``MonarchSavingsPanel``.
@_spi(Experimental) public struct MonarchSavingsItem: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The item's name, such as the asset or subscription.
    public let name: String
    /// The category status key, shown as an outlined badge.
    public let category: String?
    /// The next renewal or expiry date.
    public let date: Date?
    /// What renewing it costs. `nil` with `isUnpriced` shows "No price".
    public let amount: MonarchSavingsAmount?
    /// Whether the item has no price, shown as "No price".
    public let isUnpriced: Bool
    /// The decision status key, shown as a badge, such as "keep" or "let_lapse".
    public let decision: String?

    /// An unused item.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The item's name, already localised.
    ///   - category: The category status key.
    ///   - date: The next renewal or expiry date.
    ///   - amount: What renewing it costs.
    ///   - isUnpriced: Whether the item has no price.
    ///   - decision: The decision status key.
    public init(
        id: String,
        name: String,
        category: String? = nil,
        date: Date? = nil,
        amount: MonarchSavingsAmount? = nil,
        isUnpriced: Bool = false,
        decision: String? = nil
    ) {
        self.id = id
        self.name = name
        self.category = category
        self.date = date
        self.amount = amount
        self.isUnpriced = isUnpriced
        self.decision = decision
    }
}
