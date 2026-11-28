import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMarketingHeroFixtures: ComponentFixtures {
    static let id = "marketing-hero"
    static let fixtures = [
        Fixture("centred with actions and a note") {
            MonarchMarketingHero(
                "Write it once. Find it whenever.",
                eyebrow: "Rook Example",
                lead: "Structured documents for teams whose decisions need to stay connected.",
                note: "Set up in under five minutes."
            ) {
                MonarchButton("Start writing", variant: .primary) {}
                MonarchButton("See how it works") {}
            }
        },
        Fixture("leading, without a backdrop or actions") {
            MonarchMarketingHero(
                "Plan the quarter in one place.",
                lead: "Goals, owners and dates.",
                alignment: .leading,
                showsBackdrop: false
            )
        },
        Fixture("title only") {
            MonarchMarketingHero("Welcome")
        },
    ]
}

@Suite("MonarchMarketingHero")
struct MonarchMarketingHeroTests {
    @Test("The title uses the display role, the largest on the screen")
    func titleStyle() {
        #expect(MonarchMarketingHeroTokens.titleStyle == .display)
        #expect(MonarchMarketingHeroTokens.titleStyle.spec.size > MonarchMarketingSectionTokens.titleStyle.spec.size)
        #expect(MonarchMarketingHeroTokens.leadWeight == .regular)
        #expect(MonarchMarketingHeroTokens.noteStyle.spec.size < MonarchMarketingHeroTokens.leadStyle.spec.size)
    }

    @Test("Colours come from theme roles")
    func colours() {
        #expect(MonarchMarketingHeroTokens.eyebrowColor == \MonarchColorTokens.accentStrong)
        #expect(MonarchMarketingHeroTokens.titleColor == \MonarchColorTokens.textDefault)
        #expect(MonarchMarketingHeroTokens.supportingColor == \MonarchColorTokens.textMuted)
        #expect(MonarchMarketingHeroTokens.backdropColor == \MonarchColorTokens.accentSubtle)
    }

    @Test("A hero with a lead is taller than the title alone")
    @MainActor
    func leadAddsHeight() {
        let plain = Host.layout(MonarchMarketingHero("Welcome"))
        let led = Host.layout(MonarchMarketingHero("Welcome", lead: "Goals, owners and dates."))
        #expect(led.height > plain.height)
    }
}
