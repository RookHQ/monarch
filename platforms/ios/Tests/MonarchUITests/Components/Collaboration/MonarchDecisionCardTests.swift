import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDecisionCardFixtures: ComponentFixtures {
    static let id = "decision-card"

    static let options = [
        MonarchDecisionOption("Keep", value: "keep", description: "Renews for another year.", tone: .success),
        MonarchDecisionOption("Review", value: "review", note: "The owner is asked to confirm.", tone: .warning),
        MonarchDecisionOption(
            "Let lapse",
            value: "lapse",
            description: "Access ends on the renewal date and the data is deleted after thirty days.",
            tone: .danger,
            isReasonRequired: true
        ),
        MonarchDecisionOption("Transfer", value: "transfer", isDisabled: true),
    ]

    static let fixtures = [
        Fixture("form, nothing decided") {
            MonarchDecisionCard("Renewal decision", options: options, value: nil, onDecide: { _ in })
        },
        Fixture("form, saved decision with reason, time and person") {
            MonarchDecisionCard(
                "Renewal decision",
                options: options,
                value: "review",
                reason: "Usage fell by half since March.",
                decidedAt: Date(timeIntervalSinceNow: -7200),
                decidedBy: "Ana Flores",
                onDecide: { _ in }
            )
        },
        Fixture("form with context, no title, no empty choice, reason required") {
            MonarchDecisionCard(
                options: options,
                value: "lapse",
                allowsNone: false,
                isReasonRequired: true,
                labels: MonarchDecisionCardLabels(save: "Speichern"),
                reasonLimit: 40,
                onDecide: { _ in },
                context: {
                    Text("12 of 40 seats were used in the last 90 days.").monarchTextStyle(.body)
                }
            )
        },
        Fixture("read-only, decided") {
            MonarchDecisionCard(
                "Renewal decision",
                options: options,
                value: "lapse",
                reason: "Nobody has signed in since the migration.",
                decidedAt: Date(timeIntervalSinceNow: -86400),
                decidedBy: "Ana Flores",
                isReadOnly: true
            )
        },
        Fixture("read-only, undecided") {
            MonarchDecisionCard("Renewal decision", options: options, value: nil)
        },
    ]
}

@Suite("MonarchDecisionCard")
struct MonarchDecisionCardTests {
    typealias Tokens = MonarchDecisionCardTokens

    private let options = MonarchDecisionCardFixtures.options
    private let labels = MonarchDecisionCardLabels()
    private let english = Locale(identifier: "en_US")

    private struct Described: LocalizedError {
        var errorDescription: String? { "The renewal is locked." }
    }

    private struct Plain: Error {}

    @Test("A reason is needed when the option or the card asks for one, never for no decision")
    func needsReason() {
        #expect(!Tokens.needsReason(choice: nil, options: options, isReasonRequired: true))
        #expect(!Tokens.needsReason(choice: "keep", options: options, isReasonRequired: false))
        #expect(Tokens.needsReason(choice: "keep", options: options, isReasonRequired: true))
        #expect(Tokens.needsReason(choice: "lapse", options: options, isReasonRequired: false))
        let optional = [MonarchDecisionOption("Keep", value: "keep", isReasonRequired: false)]
        #expect(!Tokens.needsReason(choice: "keep", options: optional, isReasonRequired: true))
    }

    @Test("A reason of white space only is missing")
    func missingReason() {
        #expect(Tokens.isReasonMissing(choice: "lapse", text: "  \n", options: options, isReasonRequired: false))
        #expect(!Tokens.isReasonMissing(choice: "lapse", text: "Unused", options: options, isReasonRequired: false))
        #expect(!Tokens.isReasonMissing(choice: "keep", text: "", options: options, isReasonRequired: false))
    }

    @Test("The form is dirty when the choice or the trimmed reason differs from the saved decision")
    func dirty() {
        #expect(!Tokens.isDirty(choice: "keep", text: "", value: "keep", reason: nil))
        #expect(!Tokens.isDirty(choice: "keep", text: " Fine ", value: "keep", reason: "Fine"))
        #expect(Tokens.isDirty(choice: "lapse", text: "", value: "keep", reason: nil))
        #expect(Tokens.isDirty(choice: nil, text: "", value: "keep", reason: nil))
        #expect(Tokens.isDirty(choice: "keep", text: "New", value: "keep", reason: "Old"))
        #expect(!Tokens.isDirty(choice: nil, text: "", value: nil, reason: nil))
    }

    @Test("The decision carries the trimmed reason, or none when it is empty")
    func decision() {
        #expect(Tokens.decision(choice: "keep", text: "  Fine \n") == MonarchDecision(value: "keep", reason: "Fine"))
        #expect(Tokens.decision(choice: nil, text: "   ") == MonarchDecision(value: nil, reason: nil))
    }

    @Test("The reason is cut to the limit and the count shows near the limit")
    func limit() {
        #expect(Tokens.limited("abcdef", to: 4) == "abcd")
        #expect(Tokens.limited("abc", to: 4) == "abc")
        #expect(Tokens.limited("abc", to: -1).isEmpty)
        #expect(Tokens.charactersLeft(text: "abc", limit: 500) == nil)
        #expect(Tokens.charactersLeft(text: String(repeating: "a", count: 450), limit: 500) == 50)
        #expect(Tokens.charactersLeft(text: String(repeating: "a", count: 499), limit: 500) == 1)
        #expect(Tokens.charactersLeft(text: String(repeating: "a", count: 600), limit: 500) == 0)
    }

    @Test("The characters left are worded for one and for many, and can be replaced")
    func charactersLeftText() {
        #expect(Tokens.charactersLeftText(1, labels: labels) == "1 character left")
        #expect(Tokens.charactersLeftText(12, labels: labels) == "12 characters left")
        #expect(Tokens.charactersLeftText(0, labels: labels) == "0 characters left")
        let custom = MonarchDecisionCardLabels(charactersLeft: { "noch \($0)" })
        #expect(Tokens.charactersLeftText(12, labels: custom) == "noch 12")
    }

    @Test("A failed save shows the error's own description, or else the built-in text")
    func failureMessage() {
        #expect(Tokens.failureMessage(for: Described(), labels: labels) == "The renewal is locked.")
        #expect(Tokens.failureMessage(for: Plain(), labels: labels) == "That did not work. Try again.")
        let custom = MonarchDecisionCardLabels(failed: "Fehlgeschlagen")
        #expect(Tokens.failureMessage(for: Plain(), labels: custom) == "Fehlgeschlagen")
    }

    @Test("The decided line is relative to now, and names the person")
    func decidedText() {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let threeHoursAgo = now.addingTimeInterval(-3 * 3600)
        #expect(
            Tokens.decidedText(decidedAt: threeHoursAgo, decidedBy: "Ana", now: now, locale: english, labels: labels)
                == "Decided 3 hours ago by Ana"
        )
        #expect(
            Tokens.decidedText(decidedAt: threeHoursAgo, decidedBy: nil, now: now, locale: english, labels: labels)
                == "Decided 3 hours ago"
        )
        #expect(
            Tokens.decidedText(decidedAt: nil, decidedBy: "Ana", now: now, locale: english, labels: labels)
                == "Decided by Ana"
        )
        #expect(Tokens.decidedText(decidedAt: nil, decidedBy: "", now: now, locale: english, labels: labels) == nil)
        let custom = MonarchDecisionCardLabels(decided: "Entschieden von Ana")
        #expect(
            Tokens.decidedText(decidedAt: threeHoursAgo, decidedBy: "Ana", now: now, locale: english, labels: custom)
                == "Entschieden von Ana"
        )
    }

    @Test("VoiceOver reads the absolute date of the decision")
    func decidedAccessibilityLabel() throws {
        let date = Date(timeIntervalSince1970: 1_700_000_000)
        let utc = try #require(TimeZone(identifier: "UTC"))
        let spoken = try #require(
            Tokens.decidedAccessibilityLabel(
                decidedAt: date,
                decidedBy: "Ana",
                locale: english,
                timeZone: utc,
                labels: labels
            )
        )
        #expect(spoken.hasPrefix("Decided Nov 14, 2023"))
        #expect(spoken.contains("10:13"))
        #expect(spoken.hasSuffix("by Ana"))
        #expect(Tokens.decidedAccessibilityLabel(decidedAt: nil, decidedBy: nil, labels: labels) == nil)
    }

    @Test("A choice is read as its label and description")
    func choiceAccessibilityLabel() {
        #expect(Tokens.choiceAccessibilityLabel(label: "Keep", description: nil) == "Keep")
        #expect(Tokens.choiceAccessibilityLabel(label: "Keep", description: "") == "Keep")
        #expect(
            Tokens.choiceAccessibilityLabel(label: "Keep", description: "Renews for another year.")
                == "Keep, Renews for another year."
        )
    }

    @Test("Selection changes the glyph as well as the colours")
    func choiceAppearance() {
        #expect(Tokens.choiceSymbol(isSelected: true) != Tokens.choiceSymbol(isSelected: false))
        let selected = Tokens.choiceAppearance(isSelected: true, isEnabled: true)
        #expect(selected.background == \MonarchColorTokens.accentSubtle)
        #expect(selected.border == \MonarchColorTokens.accentStrong)
        #expect(selected.glyph == \MonarchColorTokens.accentStrong)
        let unselected = Tokens.choiceAppearance(isSelected: false, isEnabled: true)
        #expect(unselected.background == \MonarchColorTokens.bgSurface)
        #expect(unselected.border == \MonarchColorTokens.borderDefault)
        for isSelected in [true, false] {
            let disabled = Tokens.choiceAppearance(isSelected: isSelected, isEnabled: false)
            #expect(disabled.label == \MonarchColorTokens.textDisabled)
            #expect(disabled.glyph == \MonarchColorTokens.textDisabled)
        }
    }

    @Test("An option is found by its value")
    func optionLookup() {
        #expect(Tokens.option("lapse", in: options)?.label == "Let lapse")
        #expect(Tokens.option("unknown", in: options) == nil)
        #expect(Tokens.option(nil, in: options) == nil)
        #expect(options.map(\.id) == ["keep", "review", "lapse", "transfer"])
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchDecisionCardStrings.choices == "Decision")
        #expect(MonarchDecisionCardStrings.none == "No decision")
        #expect(MonarchDecisionCardStrings.reason == "Reason")
        #expect(MonarchDecisionCardStrings.reasonOptional == "Reason (optional)")
        #expect(MonarchDecisionCardStrings.reasonPrompt == "Why? Others will see this.")
        #expect(MonarchDecisionCardStrings.reasonRequired == "Add a reason for this decision.")
        #expect(MonarchDecisionCardStrings.save == "Save decision")
        #expect(MonarchDecisionCardStrings.saved == "Decision saved.")
        #expect(MonarchDecisionCardStrings.undecided == "No decision yet")
        #expect(MonarchDecisionCardStrings.failed == "That did not work. Try again.")
    }

    @Test("The form is taller than the read-only view, and a choice is a touch target")
    @MainActor
    func layout() {
        let form = Host.layout(MonarchDecisionCard(options: options, value: "keep", onDecide: { _ in }))
        let readOnly = Host.layout(MonarchDecisionCard(options: options, value: "keep", isReadOnly: true))
        #expect(form.height > readOnly.height)
        let row = Host.layout(
            MonarchDecisionChoiceRow(label: "Keep", description: nil, isSelected: true) {}
        )
        #expect(row.height >= MonarchAccessibility.minimumHitTarget)
    }
}
