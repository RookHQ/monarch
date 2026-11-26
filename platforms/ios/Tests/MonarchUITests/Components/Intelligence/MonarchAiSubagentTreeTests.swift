import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiSubagentTreeFixtures: ComponentFixtures {
    static let id = "ai-subagent-tree"

    static let start = Date(timeIntervalSince1970: 1_772_000_000)

    static let run = [
        MonarchAiSubagent(
            id: "lead",
            name: "Lead",
            role: "Write the market report",
            status: .working,
            activity: "Waiting for the researchers",
            stepsDone: 2,
            stepsTotal: 5,
            toolCalls: 4,
            startedAt: start,
            children: [
                MonarchAiSubagent(
                    id: "researcher",
                    name: "Researcher",
                    role: "Find the three latest filings",
                    status: .done,
                    stepsDone: 3,
                    toolCalls: 12,
                    startedAt: start,
                    endedAt: start.addingTimeInterval(180),
                    summary: "Found 3 filings from 2025.",
                    partId: "call-7"
                ),
                MonarchAiSubagent(
                    id: "analyst",
                    name: "Analyst",
                    status: .needsInput,
                    children: [
                        MonarchAiSubagent(id: "checker", name: "Fact checker", status: .waiting)
                    ]
                ),
                MonarchAiSubagent(id: "writer", name: "Writer", status: .failed, summary: "The template is missing."),
                MonarchAiSubagent(id: "editor", name: "Editor", status: .stopped),
            ]
        )
    ]

    static let fixtures = [
        Fixture("run, active branches open") {
            MonarchAiSubagentTree(run)
        },
        Fixture("selected with every action") {
            MonarchAiSubagentTree(
                run,
                selection: .constant("analyst"),
                onRespond: { _ in },
                onOpen: { _ in },
                onOpenPart: { _ in },
                onStop: { _ in }
            )
        },
        Fixture("all open") {
            MonarchAiSubagentTree(run, expandedIDs: .constant(["lead", "analyst"]))
        },
        Fixture("empty") {
            MonarchAiSubagentTree([])
        },
        Fixture("long names") {
            MonarchAiSubagentTree(
                [
                    MonarchAiSubagent(
                        id: "a",
                        name: "An agent with a name long enough to wrap onto two lines",
                        role: String(repeating: "A long assignment. ", count: 5),
                        status: .working,
                        activity: String(repeating: "Reading. ", count: 8)
                    )
                ],
                selection: .constant("a"),
                onStop: { _ in }
            )
        },
    ]
}

@Suite("MonarchAiSubagentTree")
struct MonarchAiSubagentTreeTests {
    private let us = Locale(identifier: "en_US")
    private let utc = TimeZone(identifier: "UTC") ?? .current
    private let labels = MonarchAiSubagentTreeLabels()
    private let run = MonarchAiSubagentTreeFixtures.run

    @Test("The tree is flattened parent first and counted by status")
    func counts() {
        #expect(
            MonarchAiSubagentTreeTokens.flattened(run).map(\.id)
                == ["lead", "researcher", "analyst", "checker", "writer", "editor"]
        )
        let counts = MonarchAiSubagentTreeTokens.counts(run)
        #expect(counts.total == 6)
        #expect(counts.byStatus[.working] == 1)
        #expect(counts.byStatus[.needsInput] == 1)
        #expect(counts.byStatus[.waiting] == 1)
        #expect(counts.byStatus[.done] == 1)
    }

    @Test("Branches with an active agent are open at first")
    func defaultExpanded() {
        #expect(MonarchAiSubagentTreeTokens.defaultExpanded(run) == ["lead", "analyst"])
        let quiet = [
            MonarchAiSubagent(
                id: "a",
                name: "A",
                status: .done,
                children: [MonarchAiSubagent(id: "b", name: "B", status: .done)]
            )
        ]
        #expect(MonarchAiSubagentTreeTokens.defaultExpanded(quiet).isEmpty)
        let deep = [
            MonarchAiSubagent(
                id: "a",
                name: "A",
                status: .done,
                children: [
                    MonarchAiSubagent(
                        id: "b",
                        name: "B",
                        status: .done,
                        children: [MonarchAiSubagent(id: "c", name: "C", status: .working)]
                    )
                ]
            )
        ]
        #expect(MonarchAiSubagentTreeTokens.defaultExpanded(deep) == ["a", "b"])
        #expect(MonarchAiSubagentTreeTokens.toggled(["a"], id: "b", isExpanded: true) == ["a", "b"])
        #expect(MonarchAiSubagentTreeTokens.toggled(["a"], id: "a", isExpanded: false).isEmpty)
    }

    @Test("The header sums the run up in status order")
    func summary() {
        #expect(
            MonarchAiSubagentTreeTokens.summary(run, locale: us, labels: labels)
                == "6 agents: 1 working, 1 needs you, 1 waiting, 1 done, 1 failed, 1 stopped"
        )
        let two = [
            MonarchAiSubagent(id: "a", name: "A", status: .needsInput),
            MonarchAiSubagent(id: "b", name: "B", status: .needsInput),
        ]
        #expect(MonarchAiSubagentTreeTokens.summary(two, locale: us, labels: labels) == "2 agents: 2 need you")
        let one = [MonarchAiSubagent(id: "a", name: "A", status: .done)]
        #expect(MonarchAiSubagentTreeTokens.summary(one, locale: us, labels: labels) == "1 agent: 1 done")
        #expect(MonarchAiSubagentTreeTokens.summary([], locale: us, labels: labels) == "No agents yet")
        let custom = MonarchAiSubagentTreeLabels(
            agentCount: { "\($0) bots" },
            statusCount: { _, count in "\(count) x" }
        )
        #expect(MonarchAiSubagentTreeTokens.summary(one, locale: us, labels: custom) == "1 bots: 1 x")
    }

    @Test("A row's facts are steps, tool calls and times")
    func meta() {
        let researcher = run[0].children[0]
        let start = MonarchAiSubagentTreeTokens.time(MonarchAiSubagentTreeFixtures.start, locale: us, timeZone: utc)
        let end = MonarchAiSubagentTreeTokens.time(
            MonarchAiSubagentTreeFixtures.start.addingTimeInterval(180),
            locale: us,
            timeZone: utc
        )
        #expect(start.contains("6:13"))
        #expect(
            MonarchAiSubagentTreeTokens.meta(researcher, locale: us, timeZone: utc, labels: labels)
                == ["3 steps", "12 tool calls", "\(start) to \(end)"]
        )
        #expect(
            MonarchAiSubagentTreeTokens.meta(run[0], locale: us, timeZone: utc, labels: labels)
                == ["2 of 5 steps", "4 tool calls", "Started \(start)"]
        )
        let single = MonarchAiSubagent(id: "s", name: "S", status: .working, stepsDone: 1, toolCalls: 1)
        #expect(
            MonarchAiSubagentTreeTokens.meta(single, locale: us, timeZone: utc, labels: labels) == [
                "1 step", "1 tool call",
            ]
        )
        #expect(MonarchAiSubagentTreeTokens.meta(run[0].children[3], locale: us, timeZone: utc, labels: labels).isEmpty)
    }

    @Test("VoiceOver reads the whole row; activity only while working, summary only once ended")
    func rowLabel() {
        #expect(
            MonarchAiSubagentTreeTokens.rowLabel(run[0].children[1], locale: us, timeZone: utc, labels: labels)
                == "Analyst, Needs input"
        )
        #expect(
            MonarchAiSubagentTreeTokens.rowLabel(run[0].children[2], locale: us, timeZone: utc, labels: labels)
                == "Writer, Failed, The template is missing."
        )
        let lead = MonarchAiSubagentTreeTokens.rowLabel(run[0], locale: us, timeZone: utc, labels: labels)
        #expect(lead.hasPrefix("Lead, Working, Write the market report, Waiting for the researchers, 2 of 5 steps"))
        var stopped = run[0]
        stopped.status = .stopped
        let stoppedLabel = MonarchAiSubagentTreeTokens.rowLabel(stopped, locale: us, timeZone: utc, labels: labels)
        #expect(!stoppedLabel.contains("Waiting for the researchers"))
    }

    @Test("Status changes are announced, never the first statuses")
    func changes() {
        #expect(MonarchAiSubagentTreeTokens.changes(from: nil, to: run).isEmpty)
        var next = run
        next[0].children[0].status = .failed
        next[0].children.append(MonarchAiSubagent(id: "new", name: "Reviewer", status: .needsInput))
        next[0].children.append(MonarchAiSubagent(id: "quiet", name: "Helper", status: .working))
        let before = MonarchAiSubagentTreeTokens.statuses(run)
        let changed = MonarchAiSubagentTreeTokens.changes(from: before, to: next)
        #expect(changed.map(\.id) == ["researcher", "new"])
        #expect(
            MonarchAiSubagentTreeTokens.announcement(changed, labels: labels)
                == "Researcher: Failed Reviewer: Needs input"
        )
        #expect(MonarchAiSubagentTreeTokens.announcement([], labels: labels) == nil)
    }

    @Test("Actions follow the status and the handlers")
    func actions() {
        let analyst = run[0].children[1]
        let all = MonarchAiSubagentTreeTokens.actions(
            for: analyst,
            hasRespond: true,
            hasOpen: true,
            hasOpenPart: true,
            hasStop: true
        )
        #expect(all == .init(respond: true, open: true, openPart: false, stop: false))
        let researcher = MonarchAiSubagentTreeTokens.actions(
            for: run[0].children[0],
            hasRespond: true,
            hasOpen: false,
            hasOpenPart: true,
            hasStop: true
        )
        #expect(researcher == .init(respond: false, open: false, openPart: true, stop: false))
        let lead = MonarchAiSubagentTreeTokens.actions(
            for: run[0],
            hasRespond: false,
            hasOpen: false,
            hasOpenPart: false,
            hasStop: true
        )
        #expect(lead == .init(stop: true))
        #expect(
            MonarchAiSubagentTreeTokens.actions(
                for: run[0],
                hasRespond: false,
                hasOpen: false,
                hasOpenPart: false,
                hasStop: false
            )
            .isEmpty
        )
    }

    @Test("Every status has a word, a tone and a distinct symbol")
    func statuses() {
        let symbols = Set(MonarchAiSubagentStatus.allCases.map { MonarchAiSubagentTreeTokens.look($0).symbol })
        #expect(symbols.count == MonarchAiSubagentStatus.allCases.count)
        #expect(MonarchAiSubagentTreeTokens.look(.failed).tone == .danger)
        #expect(MonarchAiSubagentTreeTokens.look(.needsInput).tone == .warning)
        for status in MonarchAiSubagentStatus.allCases {
            #expect(MonarchAiSubagentTreeStrings.status(status) != "monarch.aiSubagentTree.status.\(status.rawValue)")
        }
        #expect(MonarchAiSubagentTreeTokens.headerOrder == MonarchAiSubagentStatus.allCases)
        #expect(MonarchAiSubagentTreeTokens.avatarState(.working) == .working)
        #expect(MonarchAiSubagentTreeTokens.avatarState(.needsInput) == .waiting)
        #expect(MonarchAiSubagentTreeTokens.avatarState(.failed) == .error)
        #expect(MonarchAiSubagentTreeTokens.avatarState(.done) == .idle)
    }

    @Test("The selected row is marked by a bar as well as a fill, and the chevron mirrors")
    func appearance() {
        #expect(MonarchAiSubagentTreeTokens.selectedFill == \MonarchColorTokens.bgSubtle)
        #expect(MonarchAiSubagentTreeTokens.selectionBar == \MonarchColorTokens.textDefault)
        #expect(MonarchAiSubagentTreeTokens.chevronRotation(isExpanded: false, layoutDirection: .leftToRight) == .zero)
        #expect(
            MonarchAiSubagentTreeTokens.chevronRotation(isExpanded: true, layoutDirection: .leftToRight) == .degrees(90)
        )
        #expect(
            MonarchAiSubagentTreeTokens.chevronRotation(isExpanded: true, layoutDirection: .rightToLeft)
                == .degrees(-90)
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiSubagentTreeStrings.label == "Agents")
        #expect(MonarchAiSubagentTreeStrings.empty == "No agents yet")
        #expect(MonarchAiSubagentTreeStrings.stop == "Stop")
        #expect(MonarchAiSubagentTreeStrings.open == "Open")
        #expect(MonarchAiSubagentTreeStrings.respond == "Respond")
        #expect(MonarchAiSubagentTreeStrings.openPart == "Show in conversation")
        #expect(MonarchAiSubagentTreeStrings.actionsFor("Ra") == "Actions for Ra")
        #expect(MonarchAiSubagentTreeStrings.expand("Ra") == "Show agents under Ra")
        #expect(MonarchAiSubagentTreeStrings.collapse("Ra") == "Hide agents under Ra")
    }

    @Test("A selected agent with actions adds the action bar")
    @MainActor
    func layout() {
        let plain = Host.layout(MonarchAiSubagentTree(run))
        let selected = Host.layout(MonarchAiSubagentTree(run, selection: .constant("lead"), onStop: { _ in }))
        #expect(selected.height > plain.height)
    }
}
