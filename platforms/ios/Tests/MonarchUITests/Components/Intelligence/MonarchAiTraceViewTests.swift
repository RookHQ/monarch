import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiTraceViewFixtures: ComponentFixtures {
    static let id = "ai-trace-view"

    static let spans = [
        MonarchAiTraceSpan(id: "run", kind: .agent, name: "Answer question", status: .ok, start: 0, end: 4.2),
        MonarchAiTraceSpan(
            id: "plan",
            parentId: "run",
            kind: .model,
            name: "Plan",
            status: .ok,
            start: 0.1,
            end: 1.3,
            inputTokens: 1_200,
            outputTokens: 180,
            costMicros: 4_200,
            model: "Large model",
            attributes: ["temperature": "0.2", "attempt": "1"],
            input: "Plan the answer.",
            output: "1. Search. 2. Summarise.",
            partId: "msg-1"
        ),
        MonarchAiTraceSpan(
            id: "search",
            parentId: "run",
            kind: .tool,
            name: "search_documents",
            status: .error,
            start: 1.4,
            end: 2.0,
            error: "The search service timed out."
        ),
        MonarchAiTraceSpan(
            id: "retry",
            parentId: "run",
            kind: .retrieval,
            name: "Retry search",
            status: .ok,
            start: 2.1,
            end: 3.0
        ),
        MonarchAiTraceSpan(
            id: "fetch",
            parentId: "retry",
            kind: .tool,
            name: "fetch",
            status: .ok,
            start: 2.2,
            end: 2.9,
            costMicros: 1_500_000,
            currency: "EUR"
        ),
        MonarchAiTraceSpan(
            id: "check",
            parentId: "run",
            kind: .guardrail,
            name: "Safety check",
            status: .running,
            start: 3.1
        ),
    ]

    static let fixtures = [
        Fixture("default") {
            MonarchAiTraceView(spans, now: 4.5)
        },
        Fixture("selected row, all open") {
            MonarchAiTraceView(
                spans,
                selection: .constant("plan"),
                expandedIDs: .constant(["run", "retry"])
            )
        },
        Fixture("errors only") {
            MonarchAiTraceView(spans, filter: .constant(.errors))
        },
        Fixture("slowest first") {
            MonarchAiTraceView(spans, filter: .constant(.slowest), expandedIDs: .constant(["run", "retry"]))
        },
        Fixture("no errors") {
            MonarchAiTraceView([spans[0]], filter: .constant(.errors))
        },
        Fixture("empty") {
            MonarchAiTraceView([])
        },
        Fixture("details") {
            MonarchAiTraceViewDetail(
                span: spans[1],
                currency: "USD",
                labels: MonarchAiTraceViewLabels(),
                onOpenPart: { _ in },
                onClose: {}
            )
            .content
        },
        Fixture("details of a failure") {
            MonarchAiTraceViewDetail(
                span: spans[2],
                currency: "USD",
                labels: MonarchAiTraceViewLabels(),
                onOpenPart: nil,
                onClose: {}
            )
            .content
        },
    ]
}

@Suite("MonarchAiTraceView")
struct MonarchAiTraceViewTests {
    private let us = Locale(identifier: "en_US")
    private let labels = MonarchAiTraceViewLabels()
    private let spans = MonarchAiTraceViewFixtures.spans

    @Test("Totals add every span and end running spans at now")
    func totals() {
        let totals = MonarchAiTraceViewTokens.totals(spans, now: 5, currency: "USD")
        #expect(totals.start == 0)
        #expect(totals.end == 5)
        #expect(totals.inputTokens == 1_200)
        #expect(totals.outputTokens == 180)
        #expect(totals.totalTokens == 1_380)
        #expect(totals.errorCount == 1)
        #expect(totals.runningCount == 1)
        #expect(totals.spanCount == 6)
        #expect(totals.costs.map(\.currency) == ["USD", "EUR"])
        #expect(totals.costs.map(\.micros) == [4_200, 1_500_000])
        #expect(MonarchAiTraceViewTokens.totals(spans, now: nil, currency: "USD").end == 4.2)
        let empty = MonarchAiTraceViewTokens.totals([], now: 3, currency: "USD")
        #expect(empty.duration == 0)
        #expect(empty.costs.isEmpty)
    }

    @Test("Top-level spans, failures and their parents are open at first")
    func defaultExpanded() {
        #expect(MonarchAiTraceViewTokens.defaultExpanded(spans) == ["run", "search"])
    }

    @Test("Rows follow the open spans and the filter")
    func rows() {
        let totals = MonarchAiTraceViewTokens.totals(spans, now: nil, currency: "USD")
        let closed = MonarchAiTraceViewTokens.rows(spans, filter: .all, expanded: [], totals: totals)
        #expect(closed.map(\.id) == ["run"])
        #expect(closed[0].hasChildren && !closed[0].isExpanded)

        let open = MonarchAiTraceViewTokens.rows(spans, filter: .all, expanded: ["run", "retry"], totals: totals)
        #expect(open.map(\.id) == ["run", "plan", "search", "retry", "fetch", "check"])
        #expect(open.map(\.level) == [1, 2, 2, 2, 3, 2])

        let errors = MonarchAiTraceViewTokens.rows(spans, filter: .errors, expanded: ["run"], totals: totals)
        #expect(errors.map(\.id) == ["run", "search"])

        let slowest = MonarchAiTraceViewTokens.rows(spans, filter: .slowest, expanded: ["run"], totals: totals)
        #expect(slowest.map(\.id) == ["run", "plan", "check", "retry", "search"])
    }

    @Test("Missing parents, self parents and loops are shown at the top level")
    func badParents() {
        let odd = [
            MonarchAiTraceSpan(id: "a", parentId: "missing", kind: .other, name: "A", status: .ok, start: 0, end: 1),
            MonarchAiTraceSpan(id: "b", parentId: "b", kind: .other, name: "B", status: .ok, start: 1, end: 2),
            MonarchAiTraceSpan(id: "c", parentId: "d", kind: .other, name: "C", status: .ok, start: 2, end: 3),
            MonarchAiTraceSpan(id: "d", parentId: "c", kind: .other, name: "D", status: .ok, start: 3, end: 4),
            MonarchAiTraceSpan(id: "a", kind: .other, name: "Duplicate", status: .ok, start: 5, end: 6),
        ]
        let tree = MonarchAiTraceViewTokens.tree(odd)
        #expect(tree.children[nil]?.map(\.id) == ["a", "b", "c", "d"])
        #expect(tree.byId["a"]?.name == "A")
    }

    @Test("Bars sit on the shared scale and stay inside it")
    func bars() {
        let totals = MonarchAiTraceViewTokens.totals(spans, now: nil, currency: "USD")
        let plan = MonarchAiTraceViewTokens.bar(spans[1], totals: totals)
        #expect(abs(plan.offset - 0.1 / 4.2) < 0.0001)
        #expect(abs(plan.width - 1.2 / 4.2) < 0.0001)
        let running = MonarchAiTraceViewTokens.bar(spans[5], totals: totals)
        #expect(abs(running.offset + running.width - 1) < 0.0001)
        let zero = MonarchAiTraceViewTokens.totals([spans[0]], now: nil, currency: "USD")
        let point = MonarchAiTraceSpan(id: "p", kind: .other, name: "P", status: .ok, start: 9, end: 9)
        let place = MonarchAiTraceViewTokens.bar(point, totals: zero)
        #expect(place.offset <= 1 && place.width >= 0 && place.offset + place.width <= 1)
    }

    @Test("Bars are coloured by status first, then kind")
    func barFill() {
        #expect(MonarchAiTraceViewTokens.barFill(spans[2]) == \MonarchColorTokens.dangerSolid)
        #expect(MonarchAiTraceViewTokens.barFill(spans[5]) == \MonarchColorTokens.aiThinking)
        #expect(MonarchAiTraceViewTokens.barFill(spans[1]) == \MonarchColorTokens.aiSolid)
        #expect(MonarchAiTraceViewTokens.barFill(spans[4]) == \MonarchColorTokens.aiTool)
        #expect(MonarchAiTraceViewTokens.selectedFill == \MonarchColorTokens.bgSubtle)
        #expect(
            MonarchAiTraceViewTokens.chevronRotation(isExpanded: true, layoutDirection: .rightToLeft) == .degrees(-90)
        )
    }

    @Test("Durations, costs and tokens read like the run timeline")
    func text() {
        #expect(MonarchAiTraceViewTokens.durationText(0.85, locale: us, labels: labels) == "850 ms")
        #expect(MonarchAiTraceViewTokens.durationText(4.2, locale: us, labels: labels) == "4.2 s")
        #expect(MonarchAiTraceViewTokens.durationText(125, locale: us, labels: labels) == "2 min 5 s")
        #expect(MonarchAiTraceViewTokens.costText(4_200, currency: "USD", locale: us, labels: labels) == "$0.0042")
        #expect(MonarchAiTraceViewTokens.costText(nil, currency: "USD", locale: us, labels: labels) == "–")
        let totals = MonarchAiTraceViewTokens.totals(spans, now: nil, currency: "USD")
        #expect(MonarchAiTraceViewTokens.totalCostText(totals, locale: us, labels: labels) == "$0.0042 + €1.50")
        #expect(MonarchAiTraceViewTokens.tokens(spans[1]) == 1_380)
        #expect(MonarchAiTraceViewTokens.tokens(spans[0]) == nil)
        #expect(MonarchAiTraceViewTokens.spanDuration(spans[5], locale: us, labels: labels) == "Running")
        let custom = MonarchAiTraceViewLabels(formatDuration: { "\($0)s" })
        #expect(MonarchAiTraceViewTokens.durationText(2, locale: us, labels: custom) == "2.0s")
    }

    @Test("VoiceOver reads the row with its timing")
    func rowLabel() {
        #expect(
            MonarchAiTraceViewTokens.rowLabel(spans[1], currency: "USD", locale: us, labels: labels)
                == "Plan, Model, 1.2 s, 1,380 Tokens, $0.0042, Starts at +100 ms, takes 1.2 s"
        )
        #expect(
            MonarchAiTraceViewTokens.rowLabel(spans[2], currency: "USD", locale: us, labels: labels)
                == "search_documents, Error, Tool, 600 ms, Starts at +1.4 s, takes 600 ms"
        )
        #expect(
            MonarchAiTraceViewTokens.rowLabel(spans[5], currency: "USD", locale: us, labels: labels)
                == "Safety check, Guardrail, Running, Starts at +3.1 s, still running"
        )
        let custom = MonarchAiTraceViewLabels(timing: { start, duration in "\(start)|\(duration ?? "-")" })
        #expect(MonarchAiTraceViewTokens.timing(spans[5], locale: us, labels: custom) == "+3.1 s|-")
    }

    @Test("The details list what is known, attributes sorted")
    func details() {
        let facts = MonarchAiTraceViewTokens.detailFacts(spans[1], currency: "USD", locale: us, labels: labels)
        #expect(
            facts.map(\.name) == [
                "Status", "Kind", "Start", "End", "Duration", "Input tokens", "Output tokens", "Cost", "Model",
            ]
        )
        #expect(
            facts.map(\.value) == [
                "OK", "Model", "+100 ms", "+1.3 s", "1.2 s", "1,200", "180", "$0.0042", "Large model",
            ]
        )
        let running = MonarchAiTraceViewTokens.detailFacts(spans[5], currency: "USD", locale: us, labels: labels)
        #expect(running.map(\.name) == ["Status", "Kind", "Start", "Duration"])
        #expect(MonarchAiTraceViewTokens.attributes(spans[1]).map(\.name) == ["attempt", "temperature"])
        #expect(MonarchAiTraceViewTokens.canOpenPart(spans[1], hasHandler: true))
        #expect(!MonarchAiTraceViewTokens.canOpenPart(spans[1], hasHandler: false))
        #expect(!MonarchAiTraceViewTokens.canOpenPart(spans[2], hasHandler: true))
    }

    @Test("The empty text says whether the filter found nothing")
    func emptyText() {
        #expect(
            MonarchAiTraceViewTokens.emptyText(filter: .errors, spanCount: 3, labels: labels) == "No errors in this run"
        )
        #expect(
            MonarchAiTraceViewTokens.emptyText(filter: .errors, spanCount: 0, labels: labels) == "No spans recorded"
        )
        #expect(MonarchAiTraceViewTokens.emptyText(filter: .all, spanCount: 0, labels: labels) == "No spans recorded")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiTraceViewStrings.label == "Run trace")
        #expect(MonarchAiTraceViewStrings.filterLabel == "Show")
        #expect(MonarchAiTraceViewStrings.closeDetails == "Close")
        #expect(MonarchAiTraceViewStrings.attributes == "Attributes")
        for filter in MonarchAiTraceFilter.allCases {
            #expect(!MonarchAiTraceViewStrings.filter(filter).hasPrefix("monarch."))
        }
        for kind in MonarchAiTraceSpanKind.allCases {
            #expect(!MonarchAiTraceViewStrings.kind(kind).hasPrefix("monarch."))
        }
        for status in MonarchAiTraceSpanStatus.allCases {
            #expect(!MonarchAiTraceViewStrings.status(status).hasPrefix("monarch."))
        }
        #expect(MonarchAiTraceViewStrings.expand("fetch") == "Show spans inside fetch")
    }

    @Test("Opening spans adds rows")
    @MainActor
    func layout() {
        let closed = Host.layout(MonarchAiTraceView(spans, expandedIDs: .constant([])))
        let open = Host.layout(MonarchAiTraceView(spans, expandedIDs: .constant(["run", "retry"])))
        #expect(open.height > closed.height)
    }
}
