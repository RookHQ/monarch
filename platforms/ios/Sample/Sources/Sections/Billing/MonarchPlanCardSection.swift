import MonarchUI
import SwiftUI

struct MonarchPlanCardSection: CataloguePage {
    static let title = "PlanCard"

    @State private var chosen = "Starter"

    var body: some View {
        MonarchPlanCard(
            "Starter",
            price: "$0",
            period: "per month",
            description: "For trying things out.",
            features: ["3 projects", MonarchPlanCardFeature("Single sign-on", isIncluded: false)],
            isCurrent: chosen == "Starter"
        ) {
            MonarchButton("Choose Starter", fullWidth: true) { chosen = "Starter" }
                .disabled(chosen == "Starter")
        }

        MonarchPlanCard(
            "Team",
            price: "$12",
            period: "per seat / month",
            priceNote: "Billed yearly",
            description: "For teams that ship together.",
            featuresLabel: "Everything in Starter, plus",
            features: [
                "Unlimited projects",
                MonarchPlanCardFeature("Seats", hint: "up to 10"),
                "Single sign-on",
            ],
            badge: "Recommended",
            footer: "Cancel at any time.",
            isHighlighted: true,
            isCurrent: chosen == "Team"
        ) {
            MonarchButton("Choose Team", variant: .primary, fullWidth: true) { chosen = "Team" }
                .disabled(chosen == "Team")
        }

        MonarchPlanCard("Enterprise", price: "Contact us", description: "For larger organisations.")

        CatalogueCaption("Current plan: \(chosen)")
    }
}
