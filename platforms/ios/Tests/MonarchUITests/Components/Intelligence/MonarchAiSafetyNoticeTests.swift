import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiSafetyNoticeFixtures: ComponentFixtures {
    static let id = "ai-safety-notice"
    static let fixtures = [
        Fixture("blocked with reason, next steps, edit and link") {
            MonarchAiSafetyNotice(
                .blocked,
                reason: "The request asked for someone’s home address.",
                nextSteps: "Ask about public information instead, such as a company’s office location.",
                learnMoreURL: URL(string: "https://example.com/guidelines")
            ) {}
        },
        Fixture("every kind with default wording") {
            VStack {
                ForEach(MonarchAiSafetyNoticeKind.allCases, id: \.self) { kind in
                    MonarchAiSafetyNotice(kind)
                }
            }
        },
        Fixture("own title and message, unsafe link dropped") {
            MonarchAiSafetyNotice(
                .filtered,
                title: "Two passages were hidden",
                message: "They described how to bypass a lock in a way that could cause harm to others.",
                learnMoreURL: URL(string: "javascript:alert(1)")
            )
        },
        Fixture("own labels, edit only") {
            MonarchAiSafetyNotice(
                .modified,
                reason: "Medical dosage details were removed.",
                labels: MonarchAiSafetyNoticeLabels(reason: "Why:", editPrompt: "Change the question")
            ) {}
        },
    ]
}

@Suite("MonarchAiSafetyNotice")
struct MonarchAiSafetyNoticeTests {
    typealias Tokens = MonarchAiSafetyNoticeTokens

    @Test("Blocked is a warning; modified and filtered are information")
    func tones() {
        #expect(Tokens.tone(.blocked) == .warning)
        #expect(Tokens.tone(.modified) == .info)
        #expect(Tokens.tone(.filtered) == .info)
    }

    @Test("Each kind has its own symbol, so the kind does not depend on colour")
    func symbols() {
        let symbols = MonarchAiSafetyNoticeKind.allCases.map(Tokens.symbol)
        #expect(Set(symbols).count == MonarchAiSafetyNoticeKind.allCases.count)
    }

    @Test("The colours are the banner's colours for the tone")
    func appearance() {
        #expect(Tokens.appearance(.blocked) == MonarchBannerTokens.appearance(tone: .warning))
        #expect(Tokens.appearance(.modified) == MonarchBannerTokens.appearance(tone: .info))
        #expect(Tokens.appearance(.blocked).icon == \.warningStrong)
        #expect(Tokens.appearance(.filtered).background == \.infoSubtle)
    }

    @Test("Only http, https and mailto links are kept")
    func safeURL() {
        #expect(Tokens.safeURL(URL(string: "https://example.com/g")) != nil)
        #expect(Tokens.safeURL(URL(string: "HTTP://example.com")) != nil)
        #expect(Tokens.safeURL(URL(string: "mailto:trust@example.com")) != nil)
        #expect(Tokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(Tokens.safeURL(URL(string: "file:///etc/hosts")) == nil)
        #expect(Tokens.safeURL(URL(string: "relative/path")) == nil)
        #expect(Tokens.safeURL(nil) == nil)
    }

    @Test("The spoken text reads title, message, reason with its label, then next steps")
    func accessibilityLabel() {
        let full = Tokens.accessibilityLabel(
            title: "T",
            message: "M",
            reason: "Personal data",
            nextSteps: "Rephrase",
            reasonLabel: "Reason:"
        )
        #expect(full == "T, M, Reason: Personal data, Rephrase")
        let bare = Tokens.accessibilityLabel(title: "T", message: "M", reason: "  ", nextSteps: nil, reasonLabel: "R")
        #expect(bare == "T, M")
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiSafetyNoticeLabels(
            title: { _ in "Title" },
            message: { _ in "Body" },
            reason: "Why:",
            editPrompt: "Change",
            learnMore: "Rules"
        )
        #expect(labels.title(.blocked) == "Title")
        #expect(labels.message(.filtered) == "Body")
        #expect(labels.reason == "Why:")
        #expect(labels.editPrompt == "Change")
        #expect(labels.learnMore == "Rules")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiSafetyNoticeStrings.title(.blocked) == "This request wasn’t answered")
        #expect(MonarchAiSafetyNoticeStrings.title(.modified) == "This response was adjusted")
        #expect(MonarchAiSafetyNoticeStrings.title(.filtered) == "Some content was hidden")
        for kind in MonarchAiSafetyNoticeKind.allCases {
            #expect(!MonarchAiSafetyNoticeStrings.message(kind).hasPrefix("monarch."))
        }
        #expect(MonarchAiSafetyNoticeStrings.reason == "Reason:")
        #expect(MonarchAiSafetyNoticeStrings.editPrompt == "Edit prompt")
        #expect(MonarchAiSafetyNoticeStrings.learnMore == "Content guidelines")
    }
}
