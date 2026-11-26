import MonarchUI
import SwiftUI

struct MonarchPricingSectionSection: CataloguePage {
    static let title = "Pricing section"

    @State private var period = "monthly"

    var body: some View {
        MonarchPricingSection(
            "Plans that grow with your team",
            eyebrow: "Pricing",
            description: "Placeholder prices for documentation; pass the prices your billing system publishes.",
            plans: [
                MonarchPricingSectionPlan(
                    "starter",
                    name: "Starter",
                    description: "For individuals getting organised.",
                    prices: ["monthly": "₹A", "yearly": "₹B"],
                    features: [
                        "Up to 3 projects", "Email support",
                        MonarchPlanCardFeature("Single sign-on", isIncluded: false),
                    ]
                ),
                MonarchPricingSectionPlan(
                    "team",
                    name: "Team",
                    description: "For teams that share work every day.",
                    prices: ["monthly": "₹C", "yearly": "₹D"],
                    badge: "Recommended",
                    isHighlighted: true,
                    featuresLabel: "Everything in Starter, plus",
                    features: ["Unlimited projects", "Single sign-on", "Audit log"]
                ),
            ],
            periods: [
                MonarchPricingPeriod("monthly", label: "Monthly", priceSuffix: "/ month"),
                MonarchPricingPeriod("yearly", label: "Yearly", priceSuffix: "/ year", priceNote: "Billed yearly"),
            ],
            period: $period,
            comparison: [
                MonarchPricingFeature("projects", label: "Projects", values: ["starter": "Up to 3", "team": true])
            ],
            note: "Prices shown before taxes."
        ) { plan in
            MonarchButton("Choose \(plan.name)", variant: plan.isHighlighted ? .primary : .secondary, fullWidth: true) {
            }
        }
    }
}
