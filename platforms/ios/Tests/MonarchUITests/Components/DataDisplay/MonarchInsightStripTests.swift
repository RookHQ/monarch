import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchInsightStripFixtures: ComponentFixtures {
    static let id = "insight-strip"

    @MainActor static var insights: [MonarchInsight] {
        [
            MonarchInsight(
                id: "usage",
                title: "Usage is up 12% this month",
                why: "More seats were active than in any month this year.",
                tone: .success,
                origin: .rule
            ),
            MonarchInsight(
                id: "renewals",
                title: "3 domains expire in 30 days",
                why: "Auto-renew is off for all three.",
                tone: .warning,
                origin: .rule,
                evidence: "example.com, example.org, example.net",
                actionTitle: "Review renewals",
                action: {}
            ),
            MonarchInsight(
                id: "churn",
                title: "Renewal at risk for the largest account in the region",
                why: "Usage fell by half in the last month and two tickets are open without an answer.",
                tone: .danger,
                origin: .ai
            ),
            MonarchInsight(id: "note", title: "A new report is ready", why: "It covers last week.", origin: .ai),
            MonarchInsight(
                id: "plain",
                title: "Nothing changed",
                why: "Same as last week.",
                tone: .neutral,
                origin: .rule
            ),
        ]
    }

    static let fixtures = [
        Fixture("scrolling row with title and dismiss") {
            MonarchInsightStrip("Needs attention", insights: insights, onDismiss: { _ in })
        },
        Fixture("stack with a limit") {
            MonarchInsightStrip(insights: insights, layout: .stack, limit: 2)
        },
        Fixture("grid, given order") {
            MonarchInsightStrip(insights: insights, layout: .grid, sortsBySeverity: false)
        },
        Fixture("loading in every layout") {
            VStack {
                ForEach(MonarchInsightStripLayout.allCases, id: \.self) { layout in
                    MonarchInsightStrip(insights: [], layout: layout, isLoading: true)
                }
            }
        },
        Fixture("error with and without retry") {
            VStack {
                MonarchInsightStrip("Insights", insights: [], error: "The insights could not be loaded.", onRetry: {})
                MonarchInsightStrip(insights: [], error: "The insights could not be loaded.")
            }
        },
        Fixture("empty, with custom strings") {
            VStack {
                MonarchInsightStrip(insights: [])
                MonarchInsightStrip(insights: [], labels: MonarchInsightStripLabels(empty: "Alles erledigt."))
            }
        },
    ]
}

@Suite("MonarchInsightStrip")
@MainActor
struct MonarchInsightStripTests {
    typealias Tokens = MonarchInsightStripTokens

    private let labels = MonarchInsightStripLabels()

    @Test("Insights are ordered most severe first, keeping the given order within a tone")
    func sorting() {
        let insights = [
            MonarchInsight(id: "a", title: "A", why: "", tone: .success, origin: .rule),
            MonarchInsight(id: "b", title: "B", why: "", tone: .warning, origin: .rule),
            MonarchInsight(id: "c", title: "C", why: "", tone: .neutral, origin: .rule),
            MonarchInsight(id: "d", title: "D", why: "", tone: .danger, origin: .ai),
            MonarchInsight(id: "e", title: "E", why: "", tone: .warning, origin: .ai),
            MonarchInsight(id: "f", title: "F", why: "", tone: .info, origin: .ai),
        ]
        #expect(Tokens.sorted(insights).map(\.id) == ["d", "b", "e", "f", "a", "c"])
        #expect(Tokens.sorted([]).isEmpty)
    }

    @Test("Every tone has its own place in the order")
    func ranks() {
        #expect(Set(MonarchTone.allCases.map(Tokens.rank)).count == MonarchTone.allCases.count)
        #expect(Tokens.rank(.danger) < Tokens.rank(.warning))
        #expect(Tokens.rank(.warning) < Tokens.rank(.info))
        #expect(Tokens.rank(.info) < Tokens.rank(.success))
    }

    @Test("A limit hides the rest until the strip is expanded")
    func limit() {
        #expect(Tokens.canCollapse(count: 5, limit: 2))
        #expect(!Tokens.canCollapse(count: 2, limit: 2))
        #expect(!Tokens.canCollapse(count: 5, limit: nil))
        #expect(!Tokens.canCollapse(count: 5, limit: 0))
        #expect(!Tokens.canCollapse(count: 5, limit: -1))
        #expect(Tokens.shownCount(count: 5, limit: 2, isExpanded: false) == 2)
        #expect(Tokens.shownCount(count: 5, limit: 2, isExpanded: true) == 5)
        #expect(Tokens.shownCount(count: 1, limit: 2, isExpanded: false) == 1)
        #expect(Tokens.shownCount(count: 5, limit: nil, isExpanded: false) == 5)
        #expect(Tokens.shownCount(count: 5, limit: 0, isExpanded: false) == 5)
    }

    @Test("The toggle says how many are hidden")
    func toggleTitle() {
        #expect(Tokens.toggleTitle(hidden: 3, isExpanded: false, labels: labels) == "Show 3 more")
        #expect(Tokens.toggleTitle(hidden: 0, isExpanded: true, labels: labels) == "Show fewer")
        let custom = MonarchInsightStripLabels(showMore: { "\($0) weitere" }, showLess: "Weniger")
        #expect(Tokens.toggleTitle(hidden: 3, isExpanded: false, labels: custom) == "3 weitere")
        #expect(Tokens.toggleTitle(hidden: 3, isExpanded: true, labels: custom) == "Weniger")
    }

    @Test("An insight is a callout: the origin and the labels carry over")
    func callout() {
        #expect(Tokens.calloutOrigin(.ai) == .ai)
        #expect(Tokens.calloutOrigin(.rule) == .rule)
        let custom = MonarchInsightStripLabels(
            dismiss: "Close",
            evidence: "Data",
            ruleOrigin: "Regel",
            aiOrigin: "KI"
        )
        let mapped = Tokens.calloutLabels(custom)
        #expect(mapped == MonarchAiInsightCalloutLabels(ai: "KI", rule: "Regel", evidence: "Data", dismiss: "Close"))
        #expect(Tokens.calloutLabels(labels) == MonarchAiInsightCalloutLabels())
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchInsightStripStrings.showMore(2) == "Show 2 more")
        #expect(MonarchInsightStripStrings.showLess == "Show fewer")
        #expect(MonarchInsightStripStrings.empty == "Nothing needs attention right now.")
        #expect(MonarchInsightStripStrings.loading == "Loading insights")
        #expect(MonarchInsightStripStrings.retry == "Try again")
        #expect(MonarchInsightStripStrings.dismiss == "Dismiss insight")
        #expect(MonarchInsightStripStrings.evidence == "Show evidence")
        #expect(MonarchInsightStripStrings.ruleOrigin == "Rule-based")
        #expect(MonarchInsightStripStrings.aiOrigin == "AI insight")
    }

    @Test("A limit makes the stack shorter")
    func layout() {
        let insights = MonarchInsightStripFixtures.insights
        let all = Host.layout(MonarchInsightStrip(insights: insights, layout: .stack))
        let limited = Host.layout(MonarchInsightStrip(insights: insights, layout: .stack, limit: 1))
        #expect(limited.height > 0)
        #expect(limited.height < all.height)
    }
}
