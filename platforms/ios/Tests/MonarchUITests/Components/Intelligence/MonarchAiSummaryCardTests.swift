import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// Shared sample data for the summary card fixtures and tests.
private enum SummarySample {
    static let now = Date(timeIntervalSince1970: 1_763_100_000)
    static let summary = """
        Renewal is **in 30 days**. Usage grew 12% this quarter[^1].

        - Two open tickets
        - One overdue invoice

        [^1]: From the usage report.
        """
    static let sources = [
        MonarchAiSourcePart(id: "s1", title: "Q3 usage report", url: "https://example.com/q3", kind: .doc),
        MonarchAiSourcePart(id: "s2", title: "Invoice 1042", kind: .record),
    ]
}

enum MonarchAiSummaryCardFixtures: ComponentFixtures {
    static let id = "ai-summary-card"
    static let fixtures = [
        Fixture("ready: summary, sources, age, refresh and an extra action") {
            MonarchAiSummaryCard(
                "Account summary",
                summary: SummarySample.summary,
                generatedAt: SummarySample.now - 3 * 3_600,
                now: SummarySample.now,
                sources: SummarySample.sources,
                onRefresh: {}
            ) {
                Button("Copy") {}.buttonStyle(.monarch(.ghost, size: .sm))
            }
        },
        Fixture("generating the first summary: placeholder and busy refresh") {
            MonarchAiSummaryCard("Deal summary", summary: "", status: .generating, onRefresh: {})
        },
        Fixture("refreshing an existing summary") {
            MonarchAiSummaryCard("Deal summary", summary: "Two calls this week.", isRefreshing: true, onRefresh: {})
        },
        Fixture("stale, with a long title") {
            MonarchAiSummaryCard(
                "Summary of every support conversation with the customer since the start of the contract",
                summary: "Most questions were about invoices.",
                status: .stale,
                generatedAt: SummarySample.now - 40 * 86_400,
                now: SummarySample.now,
                onRefresh: {}
            )
        },
        Fixture("error with a detail, and error without one") {
            VStack {
                MonarchAiSummaryCard(
                    "Ticket summary",
                    summary: "",
                    status: .error,
                    errorMessage: "The model timed out."
                )
                MonarchAiSummaryCard("Ticket summary", summary: "", status: .error)
            }
        },
        Fixture("empty, own labels") {
            MonarchAiSummaryCard(
                "Notes",
                summary: "  ",
                labels: MonarchAiSummaryCardLabels(aiSummary: "Summary by AI", empty: "Nothing to summarize.")
            )
        },
    ]
}

@Suite("MonarchAiSummaryCard")
struct MonarchAiSummaryCardTests {
    typealias Tokens = MonarchAiSummaryCardTokens

    @Test("Refresh is busy while refreshing or generating")
    func busy() {
        #expect(Tokens.isBusy(status: .generating, isRefreshing: false))
        #expect(Tokens.isBusy(status: .ready, isRefreshing: true))
        #expect(!Tokens.isBusy(status: .stale, isRefreshing: false))
        #expect(!Tokens.isBusy(status: .error, isRefreshing: false))
    }

    @Test("The body is the summary, a placeholder while busy, nothing after an error, or the empty text")
    func body() {
        #expect(Tokens.body(summary: "Text", isBusy: true, status: .generating) == .summary)
        #expect(Tokens.body(summary: " \n", isBusy: true, status: .generating) == .placeholder)
        #expect(Tokens.body(summary: "", isBusy: false, status: .error) == .nothing)
        #expect(Tokens.body(summary: "", isBusy: false, status: .ready) == .empty)
        #expect(Tokens.body(summary: "", isBusy: false, status: .stale) == .empty)
    }

    @Test("The start and the end of work are announced, never the first appearance, nothing after an error")
    func announcements() {
        let labels = MonarchAiSummaryCardLabels()
        #expect(
            Tokens.announcement(wasBusy: false, isBusy: true, status: .generating, labels: labels)
                == "Generating summary…"
        )
        #expect(
            Tokens.announcement(wasBusy: true, isBusy: false, status: .ready, labels: labels) == "Summary updated."
        )
        #expect(Tokens.announcement(wasBusy: true, isBusy: false, status: .error, labels: labels) == nil)
        #expect(Tokens.announcement(wasBusy: false, isBusy: false, status: .ready, labels: labels) == nil)
        #expect(Tokens.announcement(wasBusy: true, isBusy: true, status: .generating, labels: labels) == nil)
    }

    @Test("The error banner puts the detail under the title, or the title alone")
    func errorBanner() {
        let labels = MonarchAiSummaryCardLabels()
        let detailed = Tokens.errorBanner(detail: "Timed out.", labels: labels)
        #expect(detailed.title == "The summary couldn’t be generated.")
        #expect(detailed.message == "Timed out.")
        let plain = Tokens.errorBanner(detail: " ", labels: labels)
        #expect(plain.title == nil)
        #expect(plain.message == "The summary couldn’t be generated.")
    }

    @Test("The age is relative to the given time; VoiceOver reads the full date and time")
    func age() {
        let labels = MonarchAiSummaryCardLabels()
        let locale = Locale(identifier: "en_US")
        let utc = TimeZone(identifier: "UTC") ?? .current
        let now = Date(timeIntervalSince1970: 1_763_100_000)
        #expect(
            Tokens.generatedLine(now - 3 * 3_600, now: now, locale: locale, labels: labels)
                == "Generated 3 hours ago"
        )
        let spoken = Tokens.generatedSpoken(now, locale: locale, timeZone: utc, labels: labels)
        #expect(spoken.hasPrefix("Generated November 14, 2025"))
        let french = Tokens.generatedLine(
            now - 3 * 3_600,
            now: now,
            locale: Locale(identifier: "fr_FR"),
            labels: labels
        )
        #expect(french.contains("3 heures"))
    }

    @Test("The placeholder text follows the generating label")
    func placeholder() {
        let text = Tokens.placeholderText("Working")
        #expect(text.hasPrefix("Working Working"))
        #expect(text.split(separator: " ").count == Tokens.placeholderLines * Tokens.placeholderLines)
    }

    @Test("Colour roles: default title, muted secondary text")
    func roles() {
        #expect(Tokens.titleRole == \.textDefault)
        #expect(Tokens.mutedRole == \.textMuted)
    }

    @Test("The built-in strings come from the strings table, and labels replace them")
    func strings() {
        let labels = MonarchAiSummaryCardLabels()
        #expect(labels.aiSummary == "AI summary")
        #expect(labels.refresh == "Refresh")
        #expect(labels.generated("now") == "Generated now")
        #expect(labels.stale.hasPrefix("The data changed"))
        #expect(labels.empty == "No summary yet.")
        #expect(labels.sources == "Sources")
        let own = MonarchAiSummaryCardLabels(
            aiSummary: "A",
            refresh: "R",
            generated: { "G " + $0 },
            generating: "W",
            updated: "U",
            stale: "S",
            error: "E",
            empty: "N",
            sources: "O"
        )
        #expect(
            [own.aiSummary, own.refresh, own.generated("x"), own.generating, own.updated, own.stale, own.error]
                == ["A", "R", "G x", "W", "U", "S", "E"]
        )
        #expect([own.empty, own.sources] == ["N", "O"])
    }
}
