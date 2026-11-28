/// One plan of a ``MonarchPricingSection``: what its card shows, and its column in the comparison.
public struct MonarchPricingSectionPlan: Sendable, Hashable, Identifiable {
    /// A stable id, used for prices per period and comparison values.
    public let id: String
    /// The plan name.
    public let name: String
    /// Who the plan is for.
    public let description: String?
    /// The price for each period id, written as the product shows it.
    public let prices: [String: String]
    /// The price when the period has none, such as "Contact us".
    public let price: String?
    /// The plan's own text after the price, instead of the period's.
    public let priceSuffix: String?
    /// A short label beside the name, such as "Recommended".
    public let badge: String?
    /// Whether the plan is emphasised.
    public let isHighlighted: Bool
    /// Whether it is the customer's current plan.
    public let isCurrent: Bool
    /// A heading above the features, such as "Everything in Starter, plus".
    public let featuresLabel: String?
    /// What the plan includes, and with `isIncluded: false` what it does not.
    public let features: [MonarchPlanCardFeature]

    /// A plan.
    ///
    /// - Parameters:
    ///   - id: A stable id, used for prices per period and comparison values.
    ///   - name: The plan name, already localised.
    ///   - description: Who the plan is for.
    ///   - prices: The price for each period id, written as the product shows it. Never formatted.
    ///   - price: The price when the period has none, such as "Contact us".
    ///   - priceSuffix: The plan's own text after the price, instead of the period's.
    ///   - badge: A short label beside the name, such as "Recommended".
    ///   - isHighlighted: Whether the plan is emphasised.
    ///   - isCurrent: Whether it is the customer's current plan.
    ///   - featuresLabel: A heading above the features.
    ///   - features: What the plan includes.
    public init(
        _ id: String,
        name: String,
        description: String? = nil,
        prices: [String: String] = [:],
        price: String? = nil,
        priceSuffix: String? = nil,
        badge: String? = nil,
        isHighlighted: Bool = false,
        isCurrent: Bool = false,
        featuresLabel: String? = nil,
        features: [MonarchPlanCardFeature] = []
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
        self.featuresLabel = featuresLabel
        self.features = features
    }

    /// The plan as a column of ``MonarchPricingTable``.
    var tablePlan: MonarchPricingPlan {
        MonarchPricingPlan(
            id,
            name: name,
            description: description,
            prices: prices,
            price: price,
            priceSuffix: priceSuffix,
            badge: badge,
            isHighlighted: isHighlighted,
            isCurrent: isCurrent
        )
    }
}
