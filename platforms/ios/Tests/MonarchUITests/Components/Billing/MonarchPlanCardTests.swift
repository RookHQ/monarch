import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPlanCardFixtures: ComponentFixtures {
    static let id = "plan-card"
    static let fixtures = [
        Fixture("highlighted with everything") {
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
                    MonarchPlanCardFeature("Single sign-on", isIncluded: false),
                ],
                badge: "Recommended",
                footer: "Cancel at any time.",
                isHighlighted: true
            ) {
                MonarchButton("Choose Team", variant: .primary, fullWidth: true) {}
            }
        },
        Fixture("current plan without an action") {
            MonarchPlanCard("Starter", price: "$0", badge: "Popular", isCurrent: true, currentPlanLabel: "Your plan")
        },
        Fixture("name only") {
            MonarchPlanCard("Enterprise")
        },
        Fixture("no price, long text") {
            MonarchPlanCard(
                "Enterprise for regulated industries and public institutions",
                price: "Contact our sales team for a quote",
                period: "billed per organisation, per year",
                features: [
                    MonarchPlanCardFeature(
                        "A dedicated support engineer in your time zone",
                        hint: "response within one working hour"
                    )
                ],
                badge: "Most complete",
                isCurrent: true
            ) {
                Button("Talk to sales") {}.buttonStyle(.monarch())
            }
        },
    ]
}

@Suite("MonarchPlanCard")
struct MonarchPlanCardTests {
    @Test("VoiceOver reads an included feature as its label and hint")
    func includedFeature() {
        #expect(
            MonarchPlanCardTokens.featureAccessibilityLabel("Unlimited projects", notIncludedLabel: "Not included:")
                == "Unlimited projects"
        )
        #expect(
            MonarchPlanCardTokens.featureAccessibilityLabel(
                MonarchPlanCardFeature("Seats", hint: "up to 10"),
                notIncludedLabel: "Not included:"
            ) == "Seats, up to 10"
        )
        #expect(
            MonarchPlanCardTokens.featureAccessibilityLabel(
                MonarchPlanCardFeature("Seats", hint: ""),
                notIncludedLabel: "Not included:"
            ) == "Seats"
        )
    }

    @Test("VoiceOver says when a feature is not included")
    func excludedFeature() {
        let feature = MonarchPlanCardFeature("Single sign-on", hint: "SAML", isIncluded: false)
        #expect(
            MonarchPlanCardTokens.featureAccessibilityLabel(feature, notIncludedLabel: "Not included:")
                == "Not included: Single sign-on, SAML"
        )
        #expect(
            MonarchPlanCardTokens.featureAccessibilityLabel(feature, notIncludedLabel: "Missing:")
                == "Missing: Single sign-on, SAML"
        )
    }

    @Test("A feature that is not included has another mark and muted text, not only another colour")
    func featureMarks() {
        #expect(MonarchPlanCardTokens.markSymbol(isIncluded: true) == "checkmark")
        #expect(MonarchPlanCardTokens.markSymbol(isIncluded: false) == "minus")

        let included = MonarchPlanCardTokens.featureRoles(isIncluded: true)
        #expect(included.mark == \MonarchColorTokens.accentStrong)
        #expect(included.text == \MonarchColorTokens.textDefault)
        let excluded = MonarchPlanCardTokens.featureRoles(isIncluded: false)
        #expect(excluded.mark == \MonarchColorTokens.textSubtle)
        #expect(excluded.text == \MonarchColorTokens.textMuted)
    }

    @Test("A highlighted card is raised, has a wider accent border and a solid badge")
    func highlight() {
        #expect(MonarchPlanCardTokens.elevation(isHighlighted: true) == .md)
        #expect(MonarchPlanCardTokens.elevation(isHighlighted: false) == .xs)
        #expect(MonarchPlanCardTokens.badgeVariant(isHighlighted: true) == .subtle)
        #expect(MonarchPlanCardTokens.badgeVariant(isHighlighted: false) == .subtle)
        #expect(MonarchPlanCardTokens.highlightBorder == \MonarchColorTokens.accentBorder)
        #expect(MonarchPlanCardTokens.highlightBorderWidth > MonarchShapes.borderWidth)
    }

    @Test("A string literal is an included feature without a hint")
    func featureLiteral() {
        let feature: MonarchPlanCardFeature = "Unlimited projects"
        #expect(feature == MonarchPlanCardFeature("Unlimited projects", hint: nil, isIncluded: true))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchPlanCardStrings.currentPlan == "Current plan")
        #expect(MonarchPlanCardStrings.notIncluded == "Not included:")
    }
}
