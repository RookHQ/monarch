import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiContextChipsFixtures: ComponentFixtures {
    static let id = "ai-context-chips"
    static let items = [
        MonarchAiContextItem(
            id: "1",
            kind: .file,
            label: "Q3 contract.pdf",
            tokens: 12_400,
            description: "Shared/Legal/Q3"
        ),
        MonarchAiContextItem(id: "2", kind: .selection, label: "Selected text", tokens: 820),
        MonarchAiContextItem(id: "3", kind: .page, label: "Invoices", tokens: 2_100, isRemovable: false),
        MonarchAiContextItem(id: "4", kind: .record, label: "Acme Corporation", tokens: 1),
        MonarchAiContextItem(id: "5", kind: .app, label: "Calendar"),
        MonarchAiContextItem(id: "6", kind: .person, label: "Dana Whitfield"),
    ]

    static let fixtures = [
        Fixture("every kind, removable") {
            MonarchAiContextChips(items, onRemove: { _ in })
        },
        Fixture("read only, against a budget") {
            MonarchAiContextChips(items, budget: 20_000)
        },
        Fixture("long name") {
            MonarchAiContextChips(
                [
                    MonarchAiContextItem(
                        id: "long",
                        kind: .file,
                        label: "Board meeting notes for the annual planning offsite, final version 7.docx",
                        tokens: 48_000
                    )
                ],
                onRemove: { _ in }
            )
        },
        Fixture("no token counts, total hidden") {
            MonarchAiContextChips(
                [MonarchAiContextItem(id: "a", kind: .app, label: "Mail")],
                labels: MonarchAiContextChipsLabels(label: "Sources")
            )
        },
        Fixture("empty with text") {
            MonarchAiContextChips([], emptyText: "Nothing is attached. Type @ to add context.")
        },
        Fixture("disabled") {
            MonarchAiContextChips(items, onRemove: { _ in }).disabled(true)
        },
    ]
}

@Suite("MonarchAiContextChips")
struct MonarchAiContextChipsTests {
    private let english = Locale(identifier: "en")

    @Test("Weights are a share of the reference")
    func weights() {
        #expect(MonarchAiContextWeight(tokens: 50, reference: 100) == .heavy)
        #expect(MonarchAiContextWeight(tokens: 49, reference: 100) == .medium)
        #expect(MonarchAiContextWeight(tokens: 20, reference: 100) == .medium)
        #expect(MonarchAiContextWeight(tokens: 19, reference: 100) == .light)
        #expect(MonarchAiContextWeight(tokens: 10, reference: 0) == .light)
        #expect(MonarchAiContextWeight(tokens: 0, reference: 100) == .light)
        #expect(MonarchAiContextWeight(tokens: -5, reference: 100) == .light)
    }

    @Test("The budget, when given, is the reference instead of the total")
    func budget() {
        let item = MonarchAiContextItem(id: "a", kind: .file, label: "a", tokens: 1_000)
        #expect(MonarchAiContextChipsTokens.weight(item, total: 1_000, budget: nil) == .heavy)
        #expect(MonarchAiContextChipsTokens.weight(item, total: 1_000, budget: 100_000) == .light)
        let unknown = MonarchAiContextItem(id: "b", kind: .file, label: "b")
        #expect(MonarchAiContextChipsTokens.weight(unknown, total: 1_000, budget: nil) == nil)
    }

    @Test("Token counts are compact")
    func formatting() {
        #expect(MonarchAiContextFormat.formatTokens(950, locale: english) == "950")
        #expect(MonarchAiContextFormat.formatTokens(1_200, locale: english) == "1.2K")
        #expect(MonarchAiContextFormat.formatTokens(18_000, locale: english) == "18K")
        #expect(MonarchAiContextFormat.formatTokens(1_100_000, locale: english) == "1.1M")
        #expect(MonarchAiContextFormat.formatTokens(-3, locale: english) == "0")
    }

    @Test("Count texts, with one token in the singular")
    func texts() {
        let labels = MonarchAiContextChipsLabels()
        #expect(MonarchAiContextChipsTokens.tokensText(1_200, labels: labels, locale: english) == "1.2K tokens")
        #expect(MonarchAiContextChipsTokens.tokensText(1, labels: labels, locale: english) == "1 token")
        #expect(
            MonarchAiContextChipsTokens.totalText(15_321, labels: labels, locale: english)
                == "≈ 15.3K tokens in context"
        )
        #expect(MonarchAiContextChipsTokens.totalText(1, labels: labels, locale: english) == "≈ 1 token in context")
        let custom = MonarchAiContextChipsLabels(tokens: { formatted, _ in "\(formatted) tk" })
        #expect(MonarchAiContextChipsTokens.tokensText(2_000, labels: custom, locale: english) == "2K tk")
    }

    @Test("The total sums known counts and is shown only when there are counts")
    func total() {
        let items = MonarchAiContextChipsFixtures.items
        #expect(MonarchAiContextChipsTokens.total(items) == 12_400 + 820 + 2_100 + 1)
        #expect(MonarchAiContextChipsTokens.showsTotal(items, override: nil))
        #expect(!MonarchAiContextChipsTokens.showsTotal(items, override: false))
        #expect(!MonarchAiContextChipsTokens.showsTotal([], override: true))
        let noCounts = [MonarchAiContextItem(id: "a", kind: .app, label: "Mail")]
        #expect(!MonarchAiContextChipsTokens.showsTotal(noCounts, override: nil))
        #expect(MonarchAiContextChipsTokens.showsTotal(noCounts, override: true))
    }

    @Test("A chip is read as kind, name and count")
    func accessibilityLabel() {
        let kind = MonarchAiContextChipsTokens.kindName(.file, labels: MonarchAiContextChipsLabels())
        #expect(kind == "File")
        #expect(
            MonarchAiContextChipsTokens.accessibilityLabel(kind: kind, label: "contract.pdf", tokens: "1.2K tokens")
                == "File: contract.pdf, 1.2K tokens"
        )
        #expect(MonarchAiContextChipsTokens.accessibilityLabel(kind: "App", label: "Mail", tokens: nil) == "App: Mail")
        let custom = MonarchAiContextChipsLabels(kind: { _ in "Fichier" })
        #expect(MonarchAiContextChipsTokens.kindName(.file, labels: custom) == "Fichier")
    }

    @Test("Every kind has its own name and icon")
    func kinds() {
        let names = MonarchAiContextKind.allCases.map { MonarchAiContextChipsStrings.kind($0) }
        #expect(Set(names).count == MonarchAiContextKind.allCases.count)
        #expect(names.allSatisfy { !$0.hasPrefix("monarch.") })
        let symbols = MonarchAiContextKind.allCases.map { MonarchAiContextFormat.symbol($0) }
        #expect(Set(symbols).count == MonarchAiContextKind.allCases.count)
    }

    @Test("Remove strings name the item")
    func removeStrings() {
        #expect(MonarchAiContextChipsStrings.remove("contract.pdf") == "Remove contract.pdf from context")
        #expect(MonarchAiContextChipsStrings.removed("contract.pdf") == "contract.pdf removed from context")
        #expect(MonarchAiContextChipsStrings.label == "Context")
    }

    @Test("Focus moves to the next chip, else the previous, else nowhere")
    func focus() {
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 1, remaining: ["a", "c"]) == "c")
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 2, remaining: ["a", "b"]) == "b")
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 0, remaining: ["b"]) == "b")
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 0, remaining: []) == nil)
    }

    @Test("The weight is drawn as filled bars, not colour alone")
    func bars() {
        #expect(MonarchAiContextChipsTokens.filledBars(.light) == 1)
        #expect(MonarchAiContextChipsTokens.filledBars(.medium) == 2)
        #expect(MonarchAiContextChipsTokens.filledBars(.heavy) == 3)
        #expect(MonarchAiContextChipsTokens.barRole(index: 1, weight: .light) == \MonarchColorTokens.borderDefault)
        #expect(MonarchAiContextChipsTokens.barRole(index: 1, weight: .medium) == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiContextChipsTokens.tokensRole(.heavy) == \MonarchColorTokens.textDefault)
        #expect(MonarchAiContextChipsTokens.tokensRole(.light) == \MonarchColorTokens.textMuted)
    }

    @Test("Chips use the AI roles, muted when disabled; removable chips reach 44 points")
    func appearance() {
        let enabled = MonarchAiContextChipsTokens.appearance(isEnabled: true)
        #expect(enabled.background == \MonarchColorTokens.aiSubtle)
        #expect(enabled.border == \MonarchColorTokens.aiBorder)
        let disabled = MonarchAiContextChipsTokens.appearance(isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        let height =
            MonarchAiContextChipsTokens.minHeight + 2 * MonarchAiContextChipsTokens.hitInset(isRemovable: true)
        #expect(height >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchAiContextChipsTokens.hitInset(isRemovable: false) == 0)
    }
}
