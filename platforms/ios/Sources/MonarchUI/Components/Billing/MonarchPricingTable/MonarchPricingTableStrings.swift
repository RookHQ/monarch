/// Built-in strings of the pricing table, from the `MonarchPricingTable` strings table.
enum MonarchPricingTableStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.pricingTable.\(key)", table: "MonarchPricingTable")
    }

    /// The spoken name of the table.
    static var caption: String { string("caption") }
    /// Spoken for a check.
    static var included: String { string("included") }
    /// Spoken for a dash.
    static var notIncluded: String { string("notIncluded") }
    /// The badge of the current plan.
    static var currentPlan: String { string("currentPlan") }
    /// The name of the billing period picker.
    static var billingPeriod: String { string("billingPeriod") }
    /// The name of the plan picker.
    static var plan: String { string("plan") }
}
