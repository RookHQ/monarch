import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiContextWindowMeterFixtures: ComponentFixtures {
    static let id = "ai-context-window-meter"
    static let fixtures = [
        Fixture("breakdown with other, near the limit, summarize") {
            MonarchAiContextWindowMeter(
                used: 110_000,
                limit: 128_000,
                breakdown: [
                    MonarchAiContextWindowPart("Instructions", tokens: 3_000),
                    MonarchAiContextWindowPart("Attachments", tokens: 22_000),
                    MonarchAiContextWindowPart("Messages", tokens: 80_000),
                ]
            ) {}
        },
        Fixture("no breakdown: room, approximate, full and summarizing") {
            VStack {
                MonarchAiContextWindowMeter(used: 45_000, limit: 128_000, isApproximate: true)
                MonarchAiContextWindowMeter(used: 130_000, limit: 128_000, isSummarizing: true) {}
            }
        },
        Fixture("six parts and a zero limit") {
            VStack {
                MonarchAiContextWindowMeter(
                    used: 6_000,
                    limit: 8_000,
                    breakdown: (1...6).map { MonarchAiContextWindowPart("Part \($0)", tokens: 1_000) }
                )
                MonarchAiContextWindowMeter(used: 10, limit: 0)
            }
        },
        Fixture("compact, from reported usage") {
            HStack {
                MonarchAiContextWindowMeter(
                    usage: MonarchAiUsage(inputTokens: 100_000, outputTokens: 4_000),
                    limit: 128_000,
                    isCompact: true
                )
                MonarchAiContextWindowMeter(used: 1_000, limit: 128_000, isCompact: true)
            }
        },
        Fixture("own labels") {
            MonarchAiContextWindowMeter(
                used: 20_000,
                limit: 32_000,
                labels: MonarchAiContextWindowMeterLabels(label: "Model memory", summarize: "Compress")
            ) {}
        },
    ]
}

@Suite("MonarchAiContextWindowMeter")
struct MonarchAiContextWindowMeterTests {
    typealias Tokens = MonarchAiContextWindowMeterTokens
    let locale = Locale(identifier: "en_US")

    @Test("The state is full at the limit, warning at the share, otherwise ok")
    func state() {
        #expect(MonarchAiContextWindowState.of(used: 10, limit: 100) == .ok)
        #expect(MonarchAiContextWindowState.of(used: 80, limit: 100) == .warning)
        #expect(MonarchAiContextWindowState.of(used: 100, limit: 100) == .full)
        #expect(MonarchAiContextWindowState.of(used: 150, limit: 100) == .full)
        #expect(MonarchAiContextWindowState.of(used: 50, limit: 100, warningAt: 0.5) == .warning)
        #expect(MonarchAiContextWindowState.of(used: 500, limit: 0) == .ok)
        #expect(MonarchAiContextWindowState.of(used: -5, limit: 100) == .ok)
    }

    @Test("Uncovered tokens become Other in neutral; parts take the data slots in order")
    func segments() {
        let segments = Tokens.segments(
            breakdown: [MonarchAiContextWindowPart("A", tokens: 10), MonarchAiContextWindowPart("B", tokens: -4)],
            used: 25,
            other: "Other"
        )
        #expect(segments.map(\.label) == ["A", "B", "Other"])
        #expect(segments.map(\.tokens) == [10, 0, 15])
        #expect(segments.map(\.color) == [.data1, .data2, .neutral])
        #expect(Tokens.segments(breakdown: [], used: 25, other: "Other").isEmpty)
        let covered = Tokens.segments(breakdown: [MonarchAiContextWindowPart("A", tokens: 30)], used: 25, other: "O")
        #expect(covered.count == 1)
        let six = Tokens.segments(
            breakdown: (1...6).map { MonarchAiContextWindowPart("P\($0)", tokens: 1) },
            used: 6,
            other: "O"
        )
        #expect(six.last?.color == .neutral)
        #expect(Set(six.map(\.id)).count == 6)
    }

    @Test("Segment widths are shares of the window, or of the total when it is larger")
    func fractions() {
        let segments = Tokens.segments(
            breakdown: [MonarchAiContextWindowPart("A", tokens: 25), MonarchAiContextWindowPart("B", tokens: 25)],
            used: 50,
            other: "O"
        )
        #expect(Tokens.fractions(segments, limit: 100) == [0.25, 0.25])
        #expect(Tokens.fractions(segments, limit: 25) == [0.5, 0.5])
        #expect(Tokens.fractions(segments, limit: 0) == [0.5, 0.5])
        #expect(Tokens.fraction(used: 150, limit: 100) == 1)
        #expect(Tokens.fraction(used: 10, limit: 0) == 0)
    }

    @Test("The fill, the notice and the button follow the state")
    func stateAppearance() {
        #expect(Tokens.fill(.ok) == \.accentSolid)
        #expect(Tokens.fill(.warning) == \.warningSolid)
        #expect(Tokens.fill(.full) == \.dangerSolid)
        #expect(Tokens.noticeTone(.ok) == nil)
        #expect(Tokens.noticeTone(.warning) == .warning)
        #expect(Tokens.noticeTone(.full) == .danger)
        #expect(Tokens.noticeSymbol(.warning) != Tokens.noticeSymbol(.full))
        #expect(Tokens.buttonVariant(.ok) == .ghost)
        #expect(Tokens.buttonVariant(.full) == .secondary)
        let labels = MonarchAiContextWindowMeterLabels()
        #expect(Tokens.notice(.ok, labels: labels) == nil)
        #expect(Tokens.notice(.warning, labels: labels) == labels.warning)
        #expect(Tokens.notice(.full, labels: labels) == labels.full)
    }

    @Test("The summary reads the amount, the size and the percentage, or About for an estimate")
    func amount() {
        let labels = MonarchAiContextWindowMeterLabels()
        let exact = Tokens.amount(used: 45_000, limit: 128_000, isApproximate: false, locale: locale, labels: labels)
        #expect(exact == "45,000 of 128,000 tokens (35%)")
        let estimate = Tokens.amount(used: 45_000, limit: 128_000, isApproximate: true, locale: locale, labels: labels)
        #expect(estimate == "About 45,000 of 128,000 tokens (35%)")
        let over = Tokens.amount(used: 130_000, limit: 128_000, isApproximate: false, locale: locale, labels: labels)
        #expect(over == "130,000 of 128,000 tokens (100%)")
    }

    @Test("Legend entries carry the colour and the amount")
    func legend() {
        let segments = Tokens.segments(
            breakdown: [MonarchAiContextWindowPart("A", tokens: 12_000)],
            used: 12_000,
            other: "O"
        )
        let items = Tokens.legendItems(segments, locale: locale, labels: MonarchAiContextWindowMeterLabels())
        #expect(items.first?.label == "A")
        #expect(items.first?.value == "12,000 tokens")
        #expect(items.first?.color == .data1)
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiContextWindowMeterLabels(
            label: "L",
            amount: { used, limit, _ in "\(used)/\(limit)" },
            approximate: { "~" + $0 },
            legend: "G",
            other: "O",
            tokens: { $0 + " t" },
            warning: "W",
            full: "F",
            summarize: "S",
            showDetails: "D"
        )
        #expect(Tokens.amount(used: 1, limit: 2, isApproximate: true, locale: locale, labels: labels) == "~1/2")
        #expect(labels.tokens("5") == "5 t")
        #expect(
            [
                labels.label, labels.legend, labels.other, labels.warning, labels.full, labels.summarize,
                labels.showDetails,
            ] == [
                "L", "G", "O", "W", "F", "S", "D",
            ]
        )
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiContextWindowMeterStrings.label == "Context window")
        #expect(MonarchAiContextWindowMeterStrings.amount("1", "2", "50%") == "1 of 2 tokens (50%)")
        #expect(MonarchAiContextWindowMeterStrings.approximate("x") == "About x")
        #expect(MonarchAiContextWindowMeterStrings.tokens("3") == "3 tokens")
        #expect(MonarchAiContextWindowMeterStrings.legend == "What fills the context window")
        #expect(MonarchAiContextWindowMeterStrings.other == "Other")
        #expect(MonarchAiContextWindowMeterStrings.summarize == "Summarize earlier messages")
        #expect(!MonarchAiContextWindowMeterStrings.warning.hasPrefix("monarch."))
        #expect(!MonarchAiContextWindowMeterStrings.full.hasPrefix("monarch."))
        #expect(!MonarchAiContextWindowMeterStrings.showDetails.hasPrefix("monarch."))
    }
}
