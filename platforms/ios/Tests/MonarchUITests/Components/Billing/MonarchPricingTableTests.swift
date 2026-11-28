import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let periods = [
    MonarchPricingPeriod("month", label: "Monthly", priceSuffix: "/ month"),
    MonarchPricingPeriod(
        "year",
        label: "Yearly",
        suffix: "2 months included",
        priceSuffix: "/ year",
        priceNote: "Billed yearly"
    ),
]

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
        priceSuffix: "per seat / month",
        badge: "Recommended",
        isHighlighted: true,
        isCurrent: true
    ),
    MonarchPricingPlan("enterprise", name: "Enterprise", prices: ["year": "$9,000"], price: "Contact us"),
]

private let features = [
    MonarchPricingFeature(
        "seats",
        label: "Seats",
        values: ["starter": "3", "team": "Unlimited", "enterprise": "Unlimited"]
    ),
    MonarchPricingFeature("projects", label: "Projects", values: ["starter": true, "team": true, "enterprise": true]),
    MonarchPricingFeature(
        "sso",
        label: "Single sign-on",
        description: "SAML and OIDC with any identity provider.",
        group: "Security",
        values: ["team": true, "enterprise": true]
    ),
    MonarchPricingFeature(
        "audit",
        label: "Audit log",
        group: "Security",
        values: ["starter": false, "enterprise": true]
    ),
    MonarchPricingFeature(
        "support",
        label: "Support",
        group: "Help",
        values: ["starter": "Email", "team": "Email and chat"]
    ),
]

enum MonarchPricingTableFixtures: ComponentFixtures {
    static let id = "pricing-table"
    static let fixtures = [
        Fixture("grid with periods, groups, badges and actions") {
            MonarchPricingTable(plans: plans, features: features, periods: periods, layout: .grid) { plan in
                MonarchButton("Choose", variant: plan.isHighlighted ? .primary : .secondary, size: .sm) {}
            }
        },
        Fixture("pages") {
            MonarchPricingTable(plans: plans, features: features, periods: periods, layout: .paged) { plan in
                MonarchButton("Choose \(plan.name)", variant: .primary, fullWidth: true) {}
            }
        },
        Fixture("automatic, yearly, no prices, one plan") {
            VStack {
                MonarchPricingTable(plans: plans, features: features, periods: periods, period: .constant("year"))
                MonarchPricingTable(plans: Array(plans.prefix(1)), features: features, isPriceShown: false)
            }
        },
    ]
}

@Suite("MonarchPricingTable")
struct MonarchPricingTableTests {
    @Test("A plan shows its period price, else its fallback")
    func prices() {
        #expect(MonarchPricingTableTokens.price(plans[1], period: "year") == "$120")
        #expect(MonarchPricingTableTokens.price(plans[2], period: "month") == "Contact us")
        #expect(MonarchPricingTableTokens.price(plans[2], period: nil) == "Contact us")
        #expect(MonarchPricingTableTokens.price(plans[0], period: "week") == nil)
    }

    @Test("Suffix and note belong to period prices, and the plan's own suffix wins")
    func suffixAndNote() {
        #expect(MonarchPricingTableTokens.priceSuffix(plans[0], period: periods[1]) == "/ year")
        #expect(MonarchPricingTableTokens.priceSuffix(plans[1], period: periods[1]) == "per seat / month")
        #expect(MonarchPricingTableTokens.priceSuffix(plans[2], period: periods[0]) == nil)
        #expect(MonarchPricingTableTokens.priceNote(plans[0], period: periods[1]) == "Billed yearly")
        #expect(MonarchPricingTableTokens.priceNote(plans[2], period: periods[0]) == nil)
        #expect(MonarchPricingTableTokens.priceNote(plans[0], period: nil) == nil)
    }

    @Test("Rows are grouped in first-seen order")
    func groups() {
        let groups = MonarchPricingTableTokens.groups(features)
        #expect(groups.map(\.title) == [nil, "Security", "Help"])
        #expect(groups.map { $0.features.map(\.id) } == [["seats", "projects"], ["sso", "audit"], ["support"]])
    }

    @Test("A plan missing from a feature's values is not included")
    func missingValue() {
        #expect(MonarchPricingTableTokens.value(features[2], plan: "starter") == .notIncluded)
        #expect(MonarchPricingTableTokens.value(features[0], plan: "team") == .text("Unlimited"))
        #expect(MonarchPricingFeatureValue(booleanLiteral: true) == .included)
    }

    @Test("Automatic is a grid only at regular width below accessibility sizes")
    func layout() {
        #expect(
            MonarchPricingTableTokens.resolvedLayout(.automatic, isRegularWidth: true, isAccessibilitySize: false)
                == .grid
        )
        #expect(
            MonarchPricingTableTokens.resolvedLayout(.automatic, isRegularWidth: false, isAccessibilitySize: false)
                == .paged
        )
        #expect(
            MonarchPricingTableTokens.resolvedLayout(.automatic, isRegularWidth: true, isAccessibilitySize: true)
                == .paged
        )
        #expect(
            MonarchPricingTableTokens.resolvedLayout(.grid, isRegularWidth: false, isAccessibilitySize: true) == .grid
        )
    }

    @Test("The period picker needs two periods and can be turned off; the first period is the default")
    func periodPicker() {
        #expect(MonarchPricingTableTokens.showsPeriodPicker(periods: 2, isShown: true))
        #expect(!MonarchPricingTableTokens.showsPeriodPicker(periods: 1, isShown: true))
        #expect(!MonarchPricingTableTokens.showsPeriodPicker(periods: 2, isShown: false))
        #expect(MonarchPricingTableTokens.selectedPeriod(bound: nil, stored: nil, periods: periods) == "month")
        #expect(MonarchPricingTableTokens.selectedPeriod(bound: nil, stored: "year", periods: periods) == "year")
        #expect(MonarchPricingTableTokens.selectedPeriod(bound: "month", stored: "year", periods: periods) == "month")
        #expect(MonarchPricingTableTokens.selectedPeriod(bound: nil, stored: nil, periods: []) == nil)
    }

    @Test("Cells and rows are read with words, never only a mark")
    func accessibility() {
        let labels = MonarchPricingTableLabels()
        #expect(
            MonarchPricingTableTokens.cellAccessibilityLabel(
                plan: "Team",
                feature: "Single sign-on",
                value: .included,
                labels: labels
            )
                == "Team, Single sign-on: Included"
        )
        #expect(
            MonarchPricingTableTokens.cellAccessibilityLabel(
                plan: "Starter",
                feature: "Audit log",
                value: .notIncluded,
                labels: labels
            )
                == "Starter, Audit log: Not included"
        )
        #expect(
            MonarchPricingTableTokens.featureAccessibilityLabel(features[2], value: .included, labels: labels)
                == "Single sign-on: Included, SAML and OIDC with any identity provider."
        )
        #expect(MonarchPricingTableTokens.valueText(.text("3"), labels: labels) == "3")
        let custom = MonarchPricingTableLabels(included: "Inklusive")
        #expect(MonarchPricingTableTokens.valueText(.included, labels: custom) == "Inklusive")
    }

    @Test("Marks: a check for included, a dash for not included, none for text")
    func marks() {
        #expect(MonarchPricingTableTokens.symbol(.included) == "checkmark")
        #expect(MonarchPricingTableTokens.symbol(.notIncluded) == "minus")
        #expect(MonarchPricingTableTokens.symbol(.text("3")) == nil)
        #expect(MonarchPricingTableTokens.markRole(.included) == \MonarchColorTokens.accentStrong)
        #expect(MonarchPricingTableTokens.markRole(.notIncluded) == \MonarchColorTokens.textSubtle)
        #expect(MonarchPricingTableTokens.labelRole(.notIncluded) == \MonarchColorTokens.textMuted)
        #expect(MonarchPricingTableTokens.labelRole(.text("x")) == \MonarchColorTokens.textDefault)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchPricingTableStrings.caption == "Plan comparison")
        #expect(MonarchPricingTableStrings.included == "Included")
        #expect(MonarchPricingTableStrings.notIncluded == "Not included")
        #expect(MonarchPricingTableStrings.currentPlan == "Current plan")
        #expect(MonarchPricingTableStrings.billingPeriod == "Billing period")
        #expect(MonarchPricingTableStrings.plan == "Plan")
    }

    @Test("The grid fits the screen width")
    @MainActor
    func gridFits() {
        let size = Host.layout(MonarchPricingTable(plans: plans, features: features, periods: periods, layout: .grid))
        #expect(size.width <= Host.width)
        #expect(size.height > 0)
    }
}
