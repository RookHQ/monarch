import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiDisclaimerFixtures: ComponentFixtures {
    static let id = "ai-disclaimer"
    static let fixtures = [
        Fixture("low") { MonarchAiDisclaimer() },
        Fixture("medium") { MonarchAiDisclaimer(risk: .medium) },
        Fixture("high") { MonarchAiDisclaimer(risk: .high) },
        Fixture("custom long text") {
            MonarchAiDisclaimer(
                "Answers are drafted from your team's shared documents and may leave out recent changes. "
                    + "Check figures against the source before you send them to a customer.",
                risk: .medium
            )
        },
    ]
}

@Suite("MonarchAiDisclaimer")
struct MonarchAiDisclaimerTests {
    @Test("Each risk has its own wording")
    func wording() {
        let texts = MonarchAiDisclaimerRisk.allCases.map { MonarchAiDisclaimerTokens.text(nil, risk: $0) }
        #expect(Set(texts).count == MonarchAiDisclaimerRisk.allCases.count)
        for text in texts {
            #expect(!text.hasPrefix("monarch."))
        }
        #expect(texts[0] == "AI can make mistakes. Check important info.")
    }

    @Test("The app's text replaces the built-in wording; empty text does not")
    func override() {
        #expect(MonarchAiDisclaimerTokens.text("Check with legal.", risk: .high) == "Check with legal.")
        #expect(MonarchAiDisclaimerTokens.text("", risk: .low) == MonarchAiDisclaimerStrings.text(.low))
    }

    @Test("Colours and layout follow the risk")
    func appearance() {
        let low = MonarchAiDisclaimerTokens.appearance(.low)
        #expect(low.text == \MonarchColorTokens.textMuted)
        #expect(low.icon == \MonarchColorTokens.aiStrong)
        let medium = MonarchAiDisclaimerTokens.appearance(.medium)
        #expect(medium.text == \MonarchColorTokens.textDefault)
        #expect(medium.icon == \MonarchColorTokens.infoStrong)
        #expect(MonarchAiDisclaimerTokens.appearance(.high).icon == \MonarchColorTokens.warningStrong)
        #expect(MonarchAiDisclaimerTokens.isCentred(.low))
        #expect(!MonarchAiDisclaimerTokens.isCentred(.medium))
        #expect(!MonarchAiDisclaimerTokens.isCentred(.high))
        #expect(MonarchAiDisclaimerTokens.textStyle(.low) == .caption)
        #expect(MonarchAiDisclaimerTokens.textStyle(.medium) == .label)
    }
}
