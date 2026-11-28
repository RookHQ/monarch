/// Replacements for the built-in strings of ``MonarchPricingTable``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPricingTableLabels: Sendable, Hashable {
    /// The spoken name of the table. Built in: "Plan comparison".
    public var caption: String?
    /// Spoken for a check. Built in: "Included".
    public var included: String?
    /// Spoken for a dash. Built in: "Not included".
    public var notIncluded: String?
    /// The badge of the current plan. Built in: "Current plan".
    public var currentPlan: String?
    /// The name of the billing period picker. Built in: "Billing period".
    public var billingPeriod: String?
    /// The name of the plan picker of the paged layout. Built in: "Plan".
    public var plan: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        caption: String? = nil,
        included: String? = nil,
        notIncluded: String? = nil,
        currentPlan: String? = nil,
        billingPeriod: String? = nil,
        plan: String? = nil
    ) {
        self.caption = caption
        self.included = included
        self.notIncluded = notIncluded
        self.currentPlan = currentPlan
        self.billingPeriod = billingPeriod
        self.plan = plan
    }
}
