/// One plan of a ``MonarchPricingTable``. Prices are shown exactly as
/// given; nothing is calculated.
public struct MonarchPricingPlan: Sendable, Hashable, Identifiable {
    /// The stable id, used as the key in every feature's values.
    public let id: String
    /// The plan name, already localised.
    public let name: String
    /// One short line on who the plan is for.
    public let description: String?
    /// The price for each billing period, keyed by ``MonarchPricingPeriod/id``,
    /// as the billing system publishes it, such as "$12".
    public let prices: [String: String]
    /// The price when `prices` has none for the period, such as "Contact us".
    public let price: String?
    /// Replaces the period's price suffix for this plan, such as "per seat / month".
    public let priceSuffix: String?
    /// A short label beside the name, such as "Recommended".
    public let badge: String?
    /// Whether the plan is emphasised as the recommended one.
    public let isHighlighted: Bool
    /// Whether this is the customer's current plan.
    public let isCurrent: Bool

    /// A plan.
    ///
    /// - Parameters:
    ///   - id: The stable id, used as the key in every feature's values.
    ///   - name: The plan name, already localised.
    ///   - description: One short line on who the plan is for.
    ///   - prices: The price for each billing period id, as published.
    ///   - price: The price when `prices` has none for the period.
    ///   - priceSuffix: Replaces the period's price suffix for this plan.
    ///   - badge: A short label beside the name.
    ///   - isHighlighted: Whether the plan is emphasised as the recommended one.
    ///   - isCurrent: Whether this is the customer's current plan.
    public init(
        _ id: String,
        name: String,
        description: String? = nil,
        prices: [String: String] = [:],
        price: String? = nil,
        priceSuffix: String? = nil,
        badge: String? = nil,
        isHighlighted: Bool = false,
        isCurrent: Bool = false
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.prices = prices
        self.price = price
        self.priceSuffix = priceSuffix
        self.badge = badge
        self.isHighlighted = isHighlighted
        self.isCurrent = isCurrent
    }
}
