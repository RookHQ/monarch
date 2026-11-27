import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPricingSectionFixtures: ComponentFixtures {
    static let id = "pricing-section"
    static let periods = [
        MonarchPricingPeriod("monthly", label: "Monthly", priceSuffix: "/ month"),
        MonarchPricingPeriod("yearly", label: "Yearly", priceSuffix: "/ year", priceNote: "Billed yearly"),
    ]
    static let plans = [
        MonarchPricingSectionPlan(
            "starter",
            name: "Starter",
            description: "For individuals getting organised.",
            prices: ["monthly": "₹A", "yearly": "₹B"],
            features: [
                "Up to 3 projects", "Email support", MonarchPlanCardFeature("Single sign-on", isIncluded: false),
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
        MonarchPricingSectionPlan(
            "enterprise",
            name: "Enterprise",
            description: "For organisations with procurement and security reviews.",
            price: "Contact us",
            featuresLabel: "Everything in Team, plus",
            features: ["Custom contract", "Dedicated onboarding"]
        ),
    ]
    static let fixtures = [
        Fixture("cards with a period picker and a note") {
            MonarchPricingSection(
                "Plans that grow with your team",
                eyebrow: "Pricing",
                plans: plans,
                periods: periods,
                note: "Prices shown before taxes."
            ) { plan in
                MonarchButton(plan.name, variant: plan.isHighlighted ? .primary : .secondary, fullWidth: true) {}
            }
        },
        Fixture("yearly, with a comparison") {
            MonarchPricingSection(
                "Plans",
                plans: plans,
                periods: periods,
                period: .constant("yearly"),
                comparison: [
                    MonarchPricingFeature(
                        "projects",
                        label: "Projects",
                        values: ["starter": "Up to 3", "team": true, "enterprise": true]
                    ),
                    MonarchPricingFeature(
                        "sso",
                        label: "Single sign-on",
                        group: "Security",
                        values: ["starter": false, "team": true, "enterprise": true]
                    ),
                ]
            )
        },
        Fixture("leading, one period, a current plan") {
            MonarchPricingSection(
                "Pricing",
                alignment: .leading,
                plans: [
                    MonarchPricingSectionPlan("free", name: "Free", price: "₹0", isCurrent: true),
                    MonarchPricingSectionPlan("pro", name: "Pro", price: "₹E", priceSuffix: "/ seat"),
                ]
            )
        },
    ]
}

@Suite("MonarchPricingSection")
struct MonarchPricingSectionTests {
    @Test("The period picker is shown only for two or more periods")
    func picker() {
        #expect(!MonarchPricingSectionTokens.showsPicker(periods: 0))
        #expect(!MonarchPricingSectionTokens.showsPicker(periods: 1))
        #expect(MonarchPricingSectionTokens.showsPicker(periods: 2))
    }

    @Test("The selected period is the bound one, else the stored one, else the first")
    func selectedPeriod() {
        let periods = MonarchPricingSectionFixtures.periods
        #expect(MonarchPricingSectionTokens.selectedPeriod(bound: nil, stored: nil, periods: periods) == "monthly")
        #expect(MonarchPricingSectionTokens.selectedPeriod(bound: nil, stored: "yearly", periods: periods) == "yearly")
        #expect(
            MonarchPricingSectionTokens.selectedPeriod(bound: "yearly", stored: "monthly", periods: periods) == "yearly"
        )
    }

    @Test("Cards sit in a row only at regular width, below accessibility sizes, for four plans at most")
    func cardsInRow() {
        #expect(
            MonarchPricingSectionTokens.placesCardsInRow(isRegularWidth: true, isAccessibilitySize: false, plans: 3)
        )
        #expect(
            !MonarchPricingSectionTokens.placesCardsInRow(isRegularWidth: false, isAccessibilitySize: false, plans: 3)
        )
        #expect(
            !MonarchPricingSectionTokens.placesCardsInRow(isRegularWidth: true, isAccessibilitySize: true, plans: 3)
        )
        #expect(
            !MonarchPricingSectionTokens.placesCardsInRow(isRegularWidth: true, isAccessibilitySize: false, plans: 5)
        )
    }

    @Test("A plan keeps its data as a comparison column")
    func tablePlan() {
        let team = MonarchPricingSectionFixtures.plans[1].tablePlan
        #expect(team.id == "team")
        #expect(team.prices["yearly"] == "₹D")
        #expect(team.badge == "Recommended")
        #expect(team.isHighlighted)
        let enterprise = MonarchPricingSectionFixtures.plans[2].tablePlan
        #expect(MonarchPricingTableTokens.price(enterprise, period: "monthly") == "Contact us")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchPricingSectionStrings.billingPeriod == "Billing period")
        #expect(MonarchPricingSectionStrings.comparisonTitle == "Compare plans")
    }
}
