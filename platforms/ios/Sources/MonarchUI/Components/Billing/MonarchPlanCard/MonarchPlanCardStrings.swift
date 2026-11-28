/// Built-in strings of the plan card, from the `MonarchPlanCard` strings table.
enum MonarchPlanCardStrings {
    /// The badge on the customer's current plan.
    static var currentPlan: String { MonarchStrings.string("monarch.planCard.currentPlan", table: "MonarchPlanCard") }
    /// Spoken before a feature the plan does not include.
    static var notIncluded: String { MonarchStrings.string("monarch.planCard.notIncluded", table: "MonarchPlanCard") }
}
