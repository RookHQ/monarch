import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiAgentRunTimelineFixtures: ComponentFixtures {
    static let id = "ai-agent-run-timeline"
    /// A fixed start, so nothing depends on the clock.
    static let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

    static let events = [
        MonarchAiRunEvent(
            id: "1",
            kind: .model,
            name: "rook-large",
            startedAt: start,
            endedAt: start.addingTimeInterval(1.2),
            inputTokens: 1_200,
            outputTokens: 310,
            costMicros: 4_100,
            partId: "text-1"
        ),
        MonarchAiRunEvent(
            id: "2",
            kind: .tool,
            name: "search_invoices",
            startedAt: start.addingTimeInterval(1.3),
            endedAt: start.addingTimeInterval(2.9),
            detail: "{\"customer\": \"C-104\"}",
            partId: "call-1"
        ),
        MonarchAiRunEvent(
            id: "3",
            kind: .handoff,
            name: "Billing agent",
            startedAt: start.addingTimeInterval(3),
            endedAt: start.addingTimeInterval(3.05)
        ),
        MonarchAiRunEvent(
            id: "4",
            kind: .error,
            name: "send_email",
            startedAt: start.addingTimeInterval(3.1),
            detail: "The mail server refused the connection."
        ),
    ]

    static let fixtures = [
        Fixture("timeline, running event, now given") {
            MonarchAiAgentRunTimeline(events, now: start.addingTimeInterval(4.2), onOpenPart: { _ in })
        },
        Fixture("table") {
            MonarchAiAgentRunTimeline(events, currency: "EUR", initialMode: .table)
        },
        Fixture("empty") {
            MonarchAiAgentRunTimeline([])
        },
        Fixture("long names, minutes") {
            MonarchAiAgentRunTimeline([
                MonarchAiRunEvent(
                    id: "a",
                    kind: .tool,
                    name: "compare_every_clause_of_the_supplier_agreement_with_the_standard_terms",
                    startedAt: start,
                    endedAt: start.addingTimeInterval(125)
                )
            ])
        },
    ]
}

@Suite("MonarchAiAgentRunTimeline")
struct MonarchAiAgentRunTimelineTests {
    private let labels = MonarchAiAgentRunTimelineLabels()
    private let locale = Locale(identifier: "en_US")
    private let start = MonarchAiAgentRunTimelineFixtures.start
    private let events = MonarchAiAgentRunTimelineFixtures.events

    @Test("Totals add tokens and cost; a running event ends at the latest known time")
    func totals() {
        let totals = MonarchAiAgentRunTimelineTokens.totals(events, now: nil)
        #expect(totals.start == start)
        #expect(totals.end == start.addingTimeInterval(3.1))
        #expect(totals.inputTokens == 1_200)
        #expect(totals.outputTokens == 310)
        #expect(totals.costMicros == 4_100)
        #expect(totals.hasCost)
    }

    @Test("An injected now extends a running event, and only then")
    func now() {
        let later = start.addingTimeInterval(10)
        #expect(MonarchAiAgentRunTimelineTokens.totals(events, now: later).end == later)
        let finished = Array(events.prefix(3))
        #expect(MonarchAiAgentRunTimelineTokens.totals(finished, now: later).end == start.addingTimeInterval(3.05))
        let empty = MonarchAiAgentRunTimelineTokens.totals([], now: nil)
        #expect(empty.duration == 0)
        #expect(!empty.hasCost)
    }

    @Test("Rows are sorted by start; bars are placed by fractions of the run")
    func rows() {
        let totals = MonarchAiAgentRunTimelineTokens.totals(events, now: start.addingTimeInterval(4))
        let rows = MonarchAiAgentRunTimelineTokens.rows(events.reversed(), totals: totals)
        #expect(rows.map(\.id) == ["1", "2", "3", "4"])
        #expect(rows[3].isRunning)
        #expect(rows[3].end == start.addingTimeInterval(4))
        #expect(MonarchAiAgentRunTimelineTokens.offsetFraction(rows[0], totals: totals) == 0)
        #expect(abs(MonarchAiAgentRunTimelineTokens.widthFraction(rows[0], totals: totals) - 0.3) < 0.0001)
        #expect(abs(MonarchAiAgentRunTimelineTokens.offsetFraction(rows[1], totals: totals) - 0.325) < 0.0001)
        let last = rows[3]
        let sum =
            MonarchAiAgentRunTimelineTokens.offsetFraction(last, totals: totals)
            + MonarchAiAgentRunTimelineTokens.widthFraction(last, totals: totals)
        #expect(sum <= 1.0001)
    }

    @Test("Durations read as ms, s, then min and s")
    func durations() {
        #expect(MonarchAiAgentRunTimelineTokens.durationText(0.85, locale: locale, labels: labels) == "850 ms")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(4.2, locale: locale, labels: labels) == "4.2 s")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(42, locale: locale, labels: labels) == "42 s")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(125, locale: locale, labels: labels) == "2 min 5 s")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(120, locale: locale, labels: labels) == "2 min")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(-1, locale: locale, labels: labels) == "0 ms")
        #expect(MonarchAiAgentRunTimelineTokens.offsetText(1.2, locale: locale, labels: labels) == "+1.2 s")
        let german = Locale(identifier: "de_DE")
        #expect(MonarchAiAgentRunTimelineTokens.durationText(4.2, locale: german, labels: labels) == "4,2 s")
        let custom = MonarchAiAgentRunTimelineLabels(formatDuration: { "\(Int($0)) sec" })
        #expect(MonarchAiAgentRunTimelineTokens.durationText(4.2, locale: locale, labels: custom) == "4 sec")
    }

    @Test("Cost uses the currency with four decimals below one unit")
    func cost() {
        #expect(
            MonarchAiAgentRunTimelineTokens.costText(4_100, currency: "USD", locale: locale, labels: labels)
                == "$0.0041"
        )
        #expect(
            MonarchAiAgentRunTimelineTokens.costText(2_500_000, currency: "USD", locale: locale, labels: labels)
                == "$2.50"
        )
        #expect(MonarchAiAgentRunTimelineTokens.costText(0, currency: "USD", locale: locale, labels: labels) == "$0.00")
        #expect(MonarchAiAgentRunTimelineTokens.costText(nil, currency: "USD", locale: locale, labels: labels) == "–")
        #expect(MonarchAiAgentRunTimelineTokens.tokensText(12_000, locale: locale, labels: labels) == "12,000")
    }

    @Test("Each event is read as its kind, name and duration")
    func rowLabel() {
        let totals = MonarchAiAgentRunTimelineTokens.totals(events, now: nil)
        let rows = MonarchAiAgentRunTimelineTokens.rows(events, totals: totals)
        #expect(
            MonarchAiAgentRunTimelineTokens.rowLabel(rows[1], locale: locale, labels: labels)
                == "Tool, search_invoices, 1.6 s"
        )
        #expect(
            MonarchAiAgentRunTimelineTokens.rowLabel(rows[3], locale: locale, labels: labels)
                == "Error, send_email, Running"
        )
        #expect(
            MonarchAiAgentRunTimelineTokens.rowLabel(rows[2], locale: locale, labels: labels)
                == "Hand-off, Billing agent, 50 ms"
        )
    }

    @Test("Open facts leave out unknowns; the table shows them")
    func facts() {
        let totals = MonarchAiAgentRunTimelineTokens.totals(events, now: nil)
        let rows = MonarchAiAgentRunTimelineTokens.rows(events, totals: totals)
        let open = MonarchAiAgentRunTimelineTokens.facts(
            rows[1],
            totals: totals,
            currency: "USD",
            locale: locale,
            labels: labels,
            includesUnknown: false
        )
        #expect(open.map(\.name) == ["Start", "Duration"])
        #expect(open.map(\.value) == ["+1.3 s", "1.6 s"])
        let table = MonarchAiAgentRunTimelineTokens.facts(
            rows[1],
            totals: totals,
            currency: "USD",
            locale: locale,
            labels: labels,
            includesUnknown: true
        )
        #expect(table.map(\.value) == ["+1.3 s", "1.6 s", "–", "–", "–"])
        #expect(
            MonarchAiAgentRunTimelineTokens.tableRowSummary(
                rows[0],
                totals: totals,
                currency: "USD",
                locale: locale,
                labels: labels
            )
                == "rook-large, Model, Start, +0 ms, Duration, 1.2 s, Input tokens, 1,200, Output tokens, 310, Cost, $0.0041"
        )
    }

    @Test("Totals list cost only when an event has one")
    func totalFacts() {
        let withCost = MonarchAiAgentRunTimelineTokens.totalFacts(
            MonarchAiAgentRunTimelineTokens.totals(events, now: nil),
            currency: "USD",
            locale: locale,
            labels: labels
        )
        #expect(withCost.map(\.name) == ["Total time", "Input tokens", "Output tokens", "Cost"])
        #expect(withCost[0].value == "3.1 s")
        let noCost = MonarchAiAgentRunTimelineTokens.totalFacts(
            MonarchAiAgentRunTimelineTokens.totals(Array(events.dropFirst()), now: nil),
            currency: "USD",
            locale: locale,
            labels: labels
        )
        #expect(noCost.count == 3)
    }

    @Test("Show in conversation needs a handler and a link")
    func openPart() {
        #expect(MonarchAiAgentRunTimelineTokens.canOpenPart(events[0], hasHandler: true))
        #expect(!MonarchAiAgentRunTimelineTokens.canOpenPart(events[0], hasHandler: false))
        #expect(!MonarchAiAgentRunTimelineTokens.canOpenPart(events[2], hasHandler: true))
    }

    @Test("Kinds have their own colours and names; errors are also named in words")
    func kinds() {
        let fills = MonarchAiRunEventKind.allCases.map(MonarchAiAgentRunTimelineTokens.barFill)
        #expect(fills == [\.aiSolid, \.aiTool, \.infoSolid, \.dangerSolid])
        #expect(MonarchAiAgentRunTimelineTokens.kindFill(.error) == \.dangerSubtle)
        #expect(MonarchAiAgentRunTimelineTokens.kindText(.error) == \.dangerStrong)
        #expect(MonarchAiAgentRunTimelineTokens.kindFill(.tool) == \.bgMuted)
        let names = MonarchAiRunEventKind.allCases.map { MonarchAiAgentRunTimelineTokens.kindName($0, labels: labels) }
        #expect(names == ["Model", "Tool", "Hand-off", "Error"])
        let modes = MonarchAiAgentRunTimelineMode.allCases.map {
            MonarchAiAgentRunTimelineTokens.modeName($0, labels: labels)
        }
        #expect(modes == ["Timeline", "Table"])
    }

    @Test("Every built-in string resolves")
    func strings() {
        #expect(MonarchAiAgentRunTimelineStrings.label == "Agent run")
        #expect(MonarchAiAgentRunTimelineStrings.empty == "No events yet")
        #expect(MonarchAiAgentRunTimelineStrings.openPart == "Show in conversation")
        #expect(MonarchAiAgentRunTimelineStrings.modeLabel == "View")
        #expect(MonarchAiAgentRunTimelineStrings.events == "Run events")
        #expect(MonarchAiAgentRunTimelineStrings.eventColumn == "Event")
        #expect(MonarchAiAgentRunTimelineStrings.kindColumn == "Kind")
    }
}
