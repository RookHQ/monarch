import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchBannerFixtures: ComponentFixtures {
    static let id = "alert"
    static let fixtures = [
        Fixture("every tone") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchBanner("Title", message: "A message long enough to wrap onto a second line.", tone: tone)
                }
            }
        },
        Fixture("no title, with action") {
            MonarchBanner(message: "No title", tone: .warning, toneLabel: "Heads up", actionTitle: "Retry") {}
        },
        Fixture("custom symbol and several actions") {
            MonarchBanner(
                "Verify your email",
                message: "We sent a link to ana@example.com.",
                tone: .warning,
                systemImage: "envelope"
            ) {
                MonarchButton("Resend", size: .sm) {}
                MonarchButton("Change email", variant: .ghost, size: .sm) {}
            }
        },
    ]
}

@Suite("MonarchBanner")
struct MonarchBannerTests {
    @Test("Colours follow the tone")
    func appearance() {
        let danger = MonarchBannerTokens.appearance(tone: .danger)
        #expect(danger.background == \MonarchColorTokens.dangerSubtle)
        #expect(danger.icon == \MonarchColorTokens.dangerStrong)
        #expect(danger.border == \MonarchColorTokens.dangerBorder)

        let neutral = MonarchBannerTokens.appearance(tone: .neutral)
        #expect(neutral.background == \MonarchColorTokens.bgSubtle)
        #expect(neutral.border == \MonarchColorTokens.borderDefault)
    }

    @Test("Each tone shows the web alert's default glyph (platforms/icons.json)")
    func symbols() {
        #expect(MonarchBannerTokens.symbol(tone: .neutral) == "info.circle")
        #expect(MonarchBannerTokens.symbol(tone: .info) == "info.circle")
        #expect(MonarchBannerTokens.symbol(tone: .success) == "checkmark.circle")
        #expect(MonarchBannerTokens.symbol(tone: .warning) == "exclamationmark.triangle")
        #expect(MonarchBannerTokens.symbol(tone: .danger) == "exclamationmark.triangle")
    }
}

@Suite("MonarchBanner symbol and actions")
@MainActor
struct MonarchBannerActionTests {
    @Test("A banner's own symbol replaces the tone's; an empty one does not")
    func symbol() {
        #expect(MonarchBannerTokens.symbol(tone: .warning, override: "envelope") == "envelope")
        #expect(MonarchBannerTokens.symbol(tone: .warning, override: "") == MonarchBannerTokens.symbol(tone: .warning))
    }

    @Test("Actions add a row under the text")
    func actions() {
        let plain = Host.layout(MonarchBanner(message: "Saved."))
        let withActions = Host.layout(
            MonarchBanner(message: "Saved.") {
                MonarchButton("Undo", size: .sm) {}
            }
        )
        #expect(withActions.height > plain.height)
    }

    @Test("the tone is spoken first, then the title and the message")
    func spokenLabel() {
        #expect(
            MonarchBannerTokens.spokenLabel(tone: "Warning", title: "Payment failed", message: "Try again.")
                == "Warning, Payment failed, Try again."
        )
        #expect(MonarchBannerTokens.spokenLabel(tone: nil, title: nil, message: "Saved.") == "Saved.")
    }

    @Test("the message is muted under a title, so the title leads")
    func messageColor() {
        #expect(MonarchBannerTokens.messageColor(hasTitle: true) == \.textMuted)
        #expect(MonarchBannerTokens.messageColor(hasTitle: false) == \.textDefault)
    }
}
