import MonarchUI
import SwiftUI

struct MonarchPricingTableSection: CataloguePage {
    static let title = "PricingTable"

    @State private var period = "month"
    @State private var layout = MonarchPricingTableLayout.automatic
    @State private var chosen = ""

    // Placeholder prices for the catalogue only.
    private let plans = [
        MonarchPricingPlan(
            "starter",
            name: "Starter",
            description: "For trying it out.",
            prices: ["month": "$0", "year": "$0"]
        ),
        MonarchPricingPlan(
            "team",
            name: "Team",
            description: "For teams that ship together.",
            prices: ["month": "$12", "year": "$120"],
            badge: "Recommended",
            isHighlighted: true
        ),
        MonarchPricingPlan("enterprise", name: "Enterprise", price: "Contact us"),
    ]

    private let features = [
        MonarchPricingFeature(
            "seats",
            label: "Seats",
            values: ["starter": "3", "team": "Unlimited", "enterprise": "Unlimited"]
        ),
        MonarchPricingFeature(
            "projects",
            label: "Projects",
            values: ["starter": true, "team": true, "enterprise": true]
        ),
        MonarchPricingFeature(
            "sso",
            label: "Single sign-on",
            description: "SAML and OIDC.",
            group: "Security",
            values: ["team": true, "enterprise": true]
        ),
        MonarchPricingFeature("audit", label: "Audit log", group: "Security", values: ["enterprise": true]),
    ]

    private let periods = [
        MonarchPricingPeriod("month", label: "Monthly", priceSuffix: "/ month"),
        MonarchPricingPeriod("year", label: "Yearly", priceSuffix: "/ year", priceNote: "Billed yearly"),
    ]

    var body: some View {
        Picker("Layout", selection: $layout) {
            Text("Automatic").tag(MonarchPricingTableLayout.automatic)
            Text("Pages").tag(MonarchPricingTableLayout.paged)
            Text("Grid").tag(MonarchPricingTableLayout.grid)
        }
        .pickerStyle(.segmented)
        MonarchPricingTable(plans: plans, features: features, periods: periods, period: $period, layout: layout) {
            plan in
            MonarchButton(
                "Choose \(plan.name)",
                variant: plan.isHighlighted ? .primary : .secondary,
                size: .sm,
                fullWidth: true
            ) {
                chosen = plan.name
            }
        }
        if !chosen.isEmpty {
            Text("Chose \(chosen), \(period)").monarchTextStyle(.caption)
        }
    }
}
