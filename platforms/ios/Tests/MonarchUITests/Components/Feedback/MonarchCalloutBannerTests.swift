import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCalloutBannerFixtures: ComponentFixtures {
    static let id = "callout-banner"
    static let fixtures = [
        Fixture("section, every tone") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCalloutBanner(
                        "Title",
                        message: "A message long enough to wrap onto a second line.",
                        tone: tone
                    )
                }
            }
        },
        Fixture("page, with action and dismiss") {
            MonarchCalloutBanner(
                "Your trial ends in 3 days",
                message: "Add a payment method to keep your workspace.",
                tone: .warning,
                placement: .page,
                toneLabel: "Heads up",
                actionTitle: "Add payment method",
                dismissLabel: "Hide",
                action: {},
                onDismiss: {}
            )
        },
        Fixture("scope") {
            VStack {
                MonarchCalloutBanner(
                    message: "Showing tickets in Support",
                    variant: .scope,
                    actionTitle: "Clear",
                    action: {}
                )
                MonarchCalloutBanner(
                    message: "Showing archived projects",
                    tone: .danger,
                    variant: .scope,
                    onDismiss: {}
                )
            }
        },
        Fixture("custom icon, no title, dismiss only") {
            MonarchCalloutBanner(message: "New: saved views.", systemImage: "sparkles", tone: .success, onDismiss: {})
        },
    ]
}

@Suite("MonarchCalloutBanner")
struct MonarchCalloutBannerTests {
    @Test("A callout takes the colours of its tone")
    func calloutAppearance() {
        let warning = MonarchCalloutBannerTokens.appearance(tone: .warning, variant: .callout)
        #expect(warning.background == \MonarchColorTokens.warningSubtle)
        #expect(warning.icon == \MonarchColorTokens.warningStrong)
        #expect(warning.border == \MonarchColorTokens.warningBorder)

        let neutral = MonarchCalloutBannerTokens.appearance(tone: .neutral, variant: .callout)
        #expect(neutral.background == \MonarchColorTokens.bgSubtle)
        #expect(neutral.border == \MonarchColorTokens.borderDefault)
    }

    @Test("The scope variant is neutral whatever the tone", arguments: MonarchTone.allCases)
    func scopeAppearance(tone: MonarchTone) {
        #expect(
            MonarchCalloutBannerTokens.appearance(tone: tone, variant: .scope)
                == MonarchCalloutBannerTokens.appearance(tone: .neutral, variant: .callout)
        )
        #expect(MonarchCalloutBannerTokens.spokenTone(tone: tone, variant: .scope, override: nil) == nil)
    }

    @Test("The icon is the app's, then the scope's, then the tone's")
    func symbol() {
        #expect(MonarchCalloutBannerTokens.symbol(tone: .info, variant: .callout, override: "sparkles") == "sparkles")
        #expect(
            MonarchCalloutBannerTokens.symbol(tone: .danger, variant: .scope, override: nil)
                == MonarchCalloutBannerTokens.scopeSymbol
        )
        #expect(
            MonarchCalloutBannerTokens.symbol(tone: .danger, variant: .callout, override: "")
                == MonarchTonePalette.palette(for: .danger).symbol
        )
    }

    @Test("The tone is spoken unless it is neutral, and can be replaced")
    func spokenTone() {
        #expect(MonarchCalloutBannerTokens.spokenTone(tone: .warning, variant: .callout, override: nil) == "Warning")
        #expect(MonarchCalloutBannerTokens.spokenTone(tone: .neutral, variant: .callout, override: nil) == nil)
        #expect(
            MonarchCalloutBannerTokens.spokenTone(tone: .warning, variant: .callout, override: "Heads up") == "Heads up"
        )
        #expect(MonarchCalloutBannerTokens.spokenTone(tone: .danger, variant: .scope, override: "Filter") == "Filter")
    }

    @Test("The scope variant is denser than the callout")
    func density() {
        #expect(
            MonarchCalloutBannerTokens.verticalPadding(.scope) < MonarchCalloutBannerTokens.verticalPadding(.callout)
        )
        #expect(MonarchCalloutBannerTokens.messageStyle(.scope) == .caption)
        // 14 pt as the web alert (text-sm), so the message never outgrows the title.
        #expect(MonarchCalloutBannerTokens.messageStyle(.callout) == .label)
        #expect(MonarchCalloutBannerTokens.messageColor(hasTitle: true) == \.textMuted)
        #expect(MonarchCalloutBannerTokens.messageColor(hasTitle: false) == \.textDefault)
    }

    @Test("The dismiss button stays inside the banner's padding", arguments: MonarchCalloutBannerVariant.allCases)
    func dismissOutset(variant: MonarchCalloutBannerVariant) {
        #expect(
            MonarchCalloutBannerTokens.dismissVerticalOutset(variant)
                <= MonarchCalloutBannerTokens.verticalPadding(variant)
        )
        #expect(MonarchCalloutBannerTokens.dismissOutset <= MonarchCalloutBannerTokens.horizontalPadding)
    }

    @Test("The dismiss name comes from the component's strings table")
    func strings() {
        #expect(MonarchCalloutBannerStrings.dismiss == "Dismiss")
    }

    @Test("A banner fills the width it is given in both placements", arguments: MonarchCalloutBannerPlacement.allCases)
    @MainActor
    func fillsWidth(placement: MonarchCalloutBannerPlacement) {
        let size = Host.layout(MonarchCalloutBanner(message: "Short", placement: placement))
        #expect(size.width == Host.width)
    }
}
