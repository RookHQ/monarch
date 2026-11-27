import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchInlineNoticeFixtures: ComponentFixtures {
    static let id = "inline-notice"
    static let fixtures = [
        Fixture("every tone in both variants") {
            VStack {
                ForEach(MonarchInlineNoticeVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchInlineNotice("A short message.", title: "Title.", tone: tone, variant: variant)
                    }
                }
            }
        },
        Fixture("action and dismiss, long message") {
            MonarchInlineNotice(
                "Someone else edited this record while you were typing, so your changes were not saved.",
                title: "Conflict.",
                tone: .danger,
                toneLabel: "Problem",
                actionTitle: "Reload",
                dismissLabel: "Hide",
                action: {},
                onDismiss: {}
            )
        },
        Fixture("plain with custom icon, under a field") {
            VStack(alignment: .leading) {
                MonarchTextField("Email", text: .constant("ada@example.com"))
                MonarchInlineNotice(
                    "This email is already invited.",
                    systemImage: "envelope.badge",
                    tone: .warning,
                    variant: .plain
                )
            }
        },
        Fixture("dismiss only") {
            MonarchInlineNotice("Saved to drafts.", tone: .success, onDismiss: {})
        },
    ]
}

@Suite("MonarchInlineNotice")
struct MonarchInlineNoticeTests {
    @Test("The subtle variant is a tinted strip with default text")
    func subtleAppearance() {
        let warning = MonarchInlineNoticeTokens.appearance(tone: .warning, variant: .subtle)
        #expect(warning.background == \MonarchColorTokens.warningSubtle)
        #expect(warning.icon == \MonarchColorTokens.warningStrong)
        #expect(warning.text == \MonarchColorTokens.textDefault)
    }

    @Test("The plain variant has no fill and its text takes the tone", arguments: MonarchTone.allCases)
    func plainAppearance(tone: MonarchTone) {
        let plain = MonarchInlineNoticeTokens.appearance(tone: tone, variant: .plain)
        #expect(plain.background == nil)
        #expect(plain.text == MonarchTonePalette.palette(for: tone).strong)
        #expect(plain.icon == plain.text)
    }

    @Test("The icon is the tone's unless the app gives one")
    func symbol() {
        let symbols = MonarchTone.allCases.map { MonarchInlineNoticeTokens.symbol(tone: $0, override: nil) }
        // Neutral shares the info circle, as on the web.
        #expect(Set(symbols).count == MonarchTone.allCases.count - 1)
        #expect(MonarchInlineNoticeTokens.symbol(tone: .info, override: "envelope.badge") == "envelope.badge")
    }

    @Test("VoiceOver reads the tone, the title, then the message")
    func accessibilityLabel() {
        #expect(
            MonarchInlineNoticeTokens.accessibilityLabel(toneName: "Warning", title: "Conflict.", message: "Reload.")
                == "Warning, Conflict., Reload."
        )
        #expect(
            MonarchInlineNoticeTokens.accessibilityLabel(toneName: nil, title: nil, message: "Saved.") == "Saved."
        )
        #expect(
            MonarchInlineNoticeTokens.accessibilityLabel(toneName: "", title: "", message: "Saved.") == "Saved."
        )
    }

    @Test("The title is emphasised and joined to the message with a space")
    func attributedText() {
        let plain = MonarchInlineNoticeTokens.attributedText(title: nil, message: "Saved.")
        #expect(String(plain.characters) == "Saved.")
        #expect(plain.runs.allSatisfy { $0.inlinePresentationIntent == nil })

        let withTitle = MonarchInlineNoticeTokens.attributedText(title: "Conflict.", message: "Reload.")
        #expect(String(withTitle.characters) == "Conflict. Reload.")
        let emphasised = withTitle.runs.filter { $0.inlinePresentationIntent == .stronglyEmphasized }
        #expect(emphasised.count == 1)
        #expect(emphasised.first.map { String(withTitle[$0.range].characters) } == "Conflict.")

        let emptyTitle = MonarchInlineNoticeTokens.attributedText(title: "", message: "Saved.")
        #expect(String(emptyTitle.characters) == "Saved.")
    }

    @Test("The dismiss name comes from the component's strings table")
    func strings() {
        #expect(MonarchInlineNoticeStrings.dismiss == "Dismiss")
    }
}
