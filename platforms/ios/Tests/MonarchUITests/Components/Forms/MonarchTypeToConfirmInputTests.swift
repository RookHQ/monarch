import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTypeToConfirmInputFixtures: ComponentFixtures {
    static let id = "type-to-confirm-input"
    static let fixtures = [
        Fixture("empty with helper") {
            MonarchTypeToConfirmInput(
                phrase: "acme-site",
                text: .constant(""),
                helper: "This deletes the site and its 42 pages."
            )
        },
        Fixture("mismatch shown") {
            MonarchTypeToConfirmInput(phrase: "acme-site", text: .constant("acme"), showsMismatch: true)
        },
        Fixture("mismatch with custom label and message") {
            MonarchTypeToConfirmInput(
                phrase: "DELETE",
                text: .constant("delete"),
                label: "Type DELETE to remove the workspace",
                showsMismatch: true,
                mismatchMessage: "Type DELETE in capital letters."
            )
        },
        Fixture("matching") {
            MonarchTypeToConfirmInput(phrase: "acme-site", text: .constant("acme-site"), showsMismatch: true)
        },
        Fixture("long phrase, disabled") {
            MonarchTypeToConfirmInput(
                phrase: "organisation-with-a-very-long-name/production-database-eu-west",
                text: .constant("")
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchTypeToConfirmInput")
struct MonarchTypeToConfirmInputTests {
    @Test("Matching is case-sensitive and ignores spaces around the text")
    func matches() {
        #expect(MonarchTypeToConfirmInput.matches("acme-site", phrase: "acme-site"))
        #expect(MonarchTypeToConfirmInput.matches("  acme-site \n", phrase: "acme-site"))
        #expect(MonarchTypeToConfirmInput.matches("acme-site", phrase: " acme-site "))
        #expect(!MonarchTypeToConfirmInput.matches("Acme-Site", phrase: "acme-site"))
        #expect(!MonarchTypeToConfirmInput.matches("acme site", phrase: "acme-site"))
        #expect(!MonarchTypeToConfirmInput.matches("", phrase: "acme-site"))
        #expect(!MonarchTypeToConfirmInput.matches("acme", phrase: "acme-site"))
    }

    @Test("The default label names the phrase and the app's label replaces it")
    func label() {
        #expect(MonarchTypeToConfirmInputTokens.label(nil, phrase: "acme-site") == "Type “acme-site” to confirm")
        #expect(MonarchTypeToConfirmInputTokens.label("Confirm", phrase: "acme-site") == "Confirm")
    }

    @Test("The error shows only when asked for and the text does not match")
    func error() {
        typealias Tokens = MonarchTypeToConfirmInputTokens
        #expect(Tokens.error(showsMismatch: false, matches: false, mismatchMessage: nil) == nil)
        #expect(Tokens.error(showsMismatch: true, matches: true, mismatchMessage: nil) == nil)
        #expect(Tokens.error(showsMismatch: true, matches: false, mismatchMessage: nil) == "The text does not match.")
        #expect(Tokens.error(showsMismatch: true, matches: false, mismatchMessage: "Try again") == "Try again")
    }

    @Test("A match is announced; a mismatch only when the app gives the text; empty text is silence")
    func announcement() {
        typealias Tokens = MonarchTypeToConfirmInputTokens
        #expect(Tokens.announcement(matches: true, matchAnnouncement: nil, mismatchAnnouncement: nil) == "Text matches")
        #expect(Tokens.announcement(matches: true, matchAnnouncement: "Ready", mismatchAnnouncement: nil) == "Ready")
        #expect(Tokens.announcement(matches: true, matchAnnouncement: "", mismatchAnnouncement: nil) == nil)
        #expect(Tokens.announcement(matches: false, matchAnnouncement: nil, mismatchAnnouncement: nil) == nil)
        #expect(Tokens.announcement(matches: false, matchAnnouncement: nil, mismatchAnnouncement: "No") == "No")
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchTypeToConfirmInputStrings.label("x") == "Type “x” to confirm")
        #expect(MonarchTypeToConfirmInputStrings.mismatch == "The text does not match.")
        #expect(MonarchTypeToConfirmInputStrings.matches == "Text matches")
    }
}
