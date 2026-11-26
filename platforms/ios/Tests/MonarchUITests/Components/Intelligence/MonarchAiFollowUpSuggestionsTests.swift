import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiFollowUpSuggestionsFixtures: ComponentFixtures {
    static let id = "ai-follow-up-suggestions"
    static let fixtures = [
        Fixture("three suggestions") {
            MonarchAiFollowUpSuggestions(
                ["What changed since last quarter?", "Show it as a table", "Draft an email"]
            ) { _ in }
        },
        Fixture("long suggestion") {
            MonarchAiFollowUpSuggestions(
                [
                    "Which customers are most likely to leave this month, and what can the account team do about it?",
                    "Summarise",
                ]
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchAiFollowUpSuggestions(["Explain the second point", "Make it shorter"]) { _ in }
                .disabled(true)
        },
        Fixture("blank and repeated suggestions with one left") {
            MonarchAiFollowUpSuggestions(["  ", "Try again", "Try again "], label: "Ideas") { _ in }
        },
    ]
}

@Suite("MonarchAiFollowUpSuggestions")
struct MonarchAiFollowUpSuggestionsTests {
    @Test("Suggestions are trimmed, blank and repeated ones are left out, and the order is kept")
    func visible() {
        let shown = MonarchAiFollowUpSuggestionsTokens.visible(["  B ", "", "A", "B", "\n", "C"])
        #expect(shown == ["B", "A", "C"])
        #expect(MonarchAiFollowUpSuggestionsTokens.visible([]).isEmpty)
        #expect(MonarchAiFollowUpSuggestionsTokens.visible([" ", ""]).isEmpty)
    }

    @Test("Chips take the AI tint while pressed and muted roles when disabled")
    func appearance() {
        let idle = MonarchAiFollowUpSuggestionsTokens.appearance(isPressed: false, isEnabled: true)
        #expect(idle.background == \MonarchColorTokens.bgSurface)
        #expect(idle.border == \MonarchColorTokens.aiBorder)
        #expect(idle.foreground == \MonarchColorTokens.aiStrong)
        let pressed = MonarchAiFollowUpSuggestionsTokens.appearance(isPressed: true, isEnabled: true)
        #expect(pressed.background == \MonarchColorTokens.aiSubtle)
        #expect(pressed.border == \MonarchColorTokens.aiBorder)
        let disabled = MonarchAiFollowUpSuggestionsTokens.appearance(isPressed: true, isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled.background == \MonarchColorTokens.bgMuted)
    }

    @Test("The touch target reaches 44 points")
    func touchTarget() {
        let total = MonarchAiFollowUpSuggestionsTokens.minHeight + 2 * MonarchAiFollowUpSuggestionsTokens.hitInset
        #expect(total >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The built-in name resolves")
    func strings() {
        #expect(MonarchAiFollowUpSuggestionsStrings.label == "Suggested follow-ups")
    }

    #if os(iOS)
        @Test("A chip is at least 44 points high")
        @MainActor func height() {
            let size = Host.layout(MonarchAiFollowUpSuggestions(["Go"]) { _ in })
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
