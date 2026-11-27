import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiMultiAgentRosterFixtures: ComponentFixtures {
    static let id = "ai-multi-agent-roster"
    static let members = [
        MonarchAiMultiAgentRosterMember(
            MonarchAiAgentRef(id: "c", name: "Coordinator", codename: "Co", role: "Plans the work"),
            state: .working,
            task: "Splitting the request"
        ),
        MonarchAiMultiAgentRosterMember(
            MonarchAiAgentRef(id: "r", name: "Research agent", codename: "Ra"),
            state: .waiting,
            task: "Needs access to the CRM"
        ),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "w", name: "Writing agent"), state: .error),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "q", name: "Review agent")),
        MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "f", name: "Fact checker"), state: .working),
    ]

    static let fixtures = [
        Fixture("list, every state") {
            MonarchAiMultiAgentRoster(members)
        },
        Fixture("list without summary") {
            MonarchAiMultiAgentRoster(Array(members.prefix(2)), showsSummary: false)
        },
        Fixture("stack with overflow") {
            MonarchAiMultiAgentRoster(members, variant: .stack, maxVisible: 2)
        },
        Fixture("stack, all visible") {
            MonarchAiMultiAgentRoster(Array(members.prefix(3)), variant: .stack)
        },
        Fixture("empty") {
            MonarchAiMultiAgentRoster([])
        },
        Fixture("long task and role") {
            MonarchAiMultiAgentRoster([
                MonarchAiMultiAgentRosterMember(
                    MonarchAiAgentRef(
                        name: "Accounts receivable reconciliation agent",
                        role: "Matches payments to open invoices across every subsidiary"
                    ),
                    state: .waiting,
                    task: "Waiting for someone to confirm the exchange rate used for the March close"
                )
            ])
        },
    ]
}

@Suite("MonarchAiMultiAgentRoster")
struct MonarchAiMultiAgentRosterTests {
    private let labels = MonarchAiMultiAgentRosterLabels()
    private let members = MonarchAiMultiAgentRosterFixtures.members

    @Test("A member is identified by the agent's id, or its name")
    func identity() {
        #expect(MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "x", name: "A")).id == "x")
        #expect(MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(name: "A")).id == "A")
    }

    @Test("Rows that need attention are tinted, and each state has its text colour")
    func colours() {
        #expect(MonarchAiMultiAgentRosterTokens.rowBackground(.idle) == nil)
        #expect(MonarchAiMultiAgentRosterTokens.rowBackground(.working) == nil)
        #expect(MonarchAiMultiAgentRosterTokens.rowBackground(.waiting) == \MonarchColorTokens.warningSubtle)
        #expect(MonarchAiMultiAgentRosterTokens.rowBackground(.error) == \MonarchColorTokens.dangerSubtle)
        #expect(MonarchAiMultiAgentRosterTokens.stateRole(.idle) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiMultiAgentRosterTokens.stateRole(.working) == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiMultiAgentRosterTokens.stateRole(.waiting) == \MonarchColorTokens.warningStrong)
        #expect(MonarchAiMultiAgentRosterTokens.stateRole(.error) == \MonarchColorTokens.dangerStrong)
    }

    @Test("The stack puts agents that need attention first and keeps order within a state")
    func stackOrder() {
        let sorted = MonarchAiMultiAgentRosterTokens.sortedForStack(members)
        #expect(sorted.map(\.id) == ["w", "r", "c", "f", "q"])
    }

    @Test("The hidden agents follow the avatar group's +N rule")
    func hidden() {
        let sorted = MonarchAiMultiAgentRosterTokens.sortedForStack(members)
        #expect(MonarchAiMultiAgentRosterTokens.hidden(sorted, maxVisible: 2).map(\.id) == ["c", "f", "q"])
        // One too many shows them all rather than "+1".
        #expect(MonarchAiMultiAgentRosterTokens.hidden(sorted, maxVisible: 4).isEmpty)
        #expect(MonarchAiMultiAgentRosterTokens.hidden(sorted, maxVisible: 0).count == 4)
        #expect(
            MonarchAiMultiAgentRosterTokens.hiddenLabel(Array(sorted.suffix(2)), labels: labels)
                == "Fact checker, Working; Review agent, Idle"
        )
    }

    @Test("The summary counts agents and the states that matter")
    func summary() {
        #expect(
            MonarchAiMultiAgentRosterTokens.summary(members, labels: labels)
                == "5 agents · 2 working · 1 waiting for you · 1 with errors"
        )
        #expect(
            MonarchAiMultiAgentRosterTokens.summary(Array(members.suffix(2).prefix(1)), labels: labels) == "1 agent"
        )
        let custom = MonarchAiMultiAgentRosterLabels(summary: { total, counts in "\(total)/\(counts[.working] ?? 0)" })
        #expect(MonarchAiMultiAgentRosterTokens.summary(members, labels: custom) == "5/2")
    }

    @Test("Changes are announced, the first state is not")
    func announcement() {
        #expect(MonarchAiMultiAgentRosterTokens.announcement(previous: nil, current: members, labels: labels) == nil)
        let before = MonarchAiMultiAgentRosterTokens.snapshot(members)
        #expect(MonarchAiMultiAgentRosterTokens.announcement(previous: before, current: members, labels: labels) == nil)
        var after = members
        after[0].state = .idle
        after[2].state = .working
        #expect(
            MonarchAiMultiAgentRosterTokens.announcement(previous: before, current: after, labels: labels)
                == "Coordinator: Idle. Writing agent: Working"
        )
        let joined = members + [MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "n", name: "New agent"))]
        #expect(
            MonarchAiMultiAgentRosterTokens.announcement(previous: before, current: joined, labels: labels)
                == "New agent: Idle"
        )
    }

    @Test("A row is read as name and kind, role, task and state")
    func rowLabel() {
        #expect(
            MonarchAiMultiAgentRosterTokens.rowLabel(members[0], labels: labels)
                == "Coordinator (AI agent), Plans the work, Splitting the request, Working"
        )
        #expect(MonarchAiMultiAgentRosterTokens.rowLabel(members[3], labels: labels) == "Review agent (AI agent), Idle")
        let custom = MonarchAiMultiAgentRosterLabels(agentKind: "bot", state: { _ in "Busy" })
        #expect(MonarchAiMultiAgentRosterTokens.rowLabel(members[3], labels: custom) == "Review agent (bot), Busy")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiMultiAgentRosterStrings.label == "Agents")
        #expect(MonarchAiMultiAgentRosterStrings.empty == "No agents are working on this.")
        #expect(MonarchAiMultiAgentRosterStrings.agents(1) == "1 agent")
        #expect(MonarchAiMultiAgentRosterStrings.agents(3) == "3 agents")
        #expect(MonarchAiMultiAgentRosterStrings.working(2) == "2 working")
        #expect(MonarchAiMultiAgentRosterStrings.waiting(1) == "1 waiting for you")
        #expect(MonarchAiMultiAgentRosterStrings.errors(4) == "4 with errors")
        #expect(MonarchAiMultiAgentRosterStrings.changed("A", "Error") == "A: Error")
        #expect(MonarchAiMultiAgentRosterStrings.agentName("A", kind: "AI agent") == "A (AI agent)")
    }
}
