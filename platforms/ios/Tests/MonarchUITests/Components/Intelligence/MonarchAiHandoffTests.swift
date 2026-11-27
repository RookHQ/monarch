import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiHandoffFixtures: ComponentFixtures {
    static let id = "ai-handoff"
    static let research = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")
    static let writer = MonarchAiAgentRef(id: "writer", name: "Writing agent", codename: "Wa")
    static let now = Date(timeIntervalSince1970: 1_772_452_800)

    static let fixtures = [
        Fixture("agent to agent with reason and time") {
            MonarchAiHandoff(
                from: research,
                to: .agent(writer),
                reason: "Sources are ready to draft from",
                date: now.addingTimeInterval(-180),
                now: now
            )
        },
        Fixture("agent to a person") {
            MonarchAiHandoff(
                from: research,
                to: .person(name: "Sam Lee"),
                reason: "Needs a person to approve the refund"
            )
        },
        Fixture("no reason, no time") {
            MonarchAiHandoff(from: research, to: .agent(writer))
        },
        Fixture("long names and reason") {
            MonarchAiHandoff(
                from: MonarchAiAgentRef(name: "Accounts receivable reconciliation agent"),
                to: .person(name: "Maria Fernanda Gonçalves de Albuquerque"),
                reason:
                    "The customer disputes three invoices and asked to speak with a person before anything is refunded.",
                date: now.addingTimeInterval(-86_400 * 3),
                now: now
            )
        },
    ]
}

@Suite("MonarchAiHandoff")
struct MonarchAiHandoffTests {
    private let labels = MonarchAiHandoffLabels()
    private static let locale = Locale(identifier: "en_US")
    private static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .gmt
        return calendar
    }

    @Test("The sentence names both sides")
    func sentence() {
        #expect(
            MonarchAiHandoffTokens.sentence(from: "Research agent", to: .person(name: "Sam Lee"), labels: labels)
                == "Research agent handed off to Sam Lee"
        )
        #expect(
            MonarchAiHandoffTokens.sentence(
                from: "Research agent",
                to: .agent(MonarchAiAgentRef(name: "Writing agent")),
                labels: labels
            ) == "Research agent handed off to Writing agent"
        )
    }

    @Test("Spoken agent names carry the kind; a person's name does not")
    func spokenSentence() {
        #expect(
            MonarchAiHandoffTokens.spokenSentence(from: "Research agent", to: .person(name: "Sam Lee"), labels: labels)
                == "Research agent (AI agent) handed off to Sam Lee"
        )
        #expect(
            MonarchAiHandoffTokens.spokenSentence(
                from: "Research agent",
                to: .agent(MonarchAiAgentRef(name: "Writing agent")),
                labels: labels
            ) == "Research agent (AI agent) handed off to Writing agent (AI agent)"
        )
    }

    @Test("The sentence and the kind can be replaced")
    func overrides() {
        let custom = MonarchAiHandoffLabels(
            handedOff: { from, to, isPerson in isPerson ? "\(to) takes over from \(from)" : "\(from) → \(to)" },
            agentKind: "bot"
        )
        #expect(
            MonarchAiHandoffTokens.spokenSentence(from: "Ra", to: .person(name: "Sam"), labels: custom)
                == "Sam takes over from Ra (bot)"
        )
        #expect(
            MonarchAiHandoffTokens.sentence(from: "Ra", to: .agent(MonarchAiAgentRef(name: "Wa")), labels: custom)
                == "Ra → Wa"
        )
    }

    @Test("The target says whether it is a person")
    func target() {
        #expect(MonarchAiHandoffTarget.person(name: "Sam").isPerson)
        #expect(!MonarchAiHandoffTarget.agent(MonarchAiAgentRef(name: "Wa")).isPerson)
        #expect(MonarchAiHandoffTarget.agent(MonarchAiAgentRef(name: "Wa")).name == "Wa")
    }

    @Test("Lines are AI for an agent and neutral for a person")
    func colours() {
        #expect(MonarchAiHandoffTokens.lineRole(isPerson: false) == \MonarchColorTokens.aiBorder)
        #expect(MonarchAiHandoffTokens.lineRole(isPerson: true) == \MonarchColorTokens.borderDefault)
        #expect(MonarchAiHandoffTokens.arrowRole(isPerson: false) == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiHandoffTokens.arrowRole(isPerson: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiHandoffTokens.showsLines(isAccessibilitySize: false))
        #expect(!MonarchAiHandoffTokens.showsLines(isAccessibilitySize: true))
    }

    @Test("The time is relative to now in the given locale")
    func time() {
        let now = Date(timeIntervalSince1970: 1_772_452_800)
        #expect(
            MonarchAiHandoffTokens.timeText(
                now.addingTimeInterval(-180),
                now: now,
                locale: Self.locale,
                calendar: Self.calendar
            ) == "3 minutes ago"
        )
        #expect(
            MonarchAiHandoffTokens.timeText(
                now.addingTimeInterval(-86_400),
                now: now,
                locale: Self.locale,
                calendar: Self.calendar
            ) == "yesterday"
        )
    }

    @Test("VoiceOver reads the marker, the sentence, the reason and the time")
    func accessibilityLabel() {
        #expect(
            MonarchAiHandoffTokens.accessibilityLabel(
                label: "Handoff",
                sentence: "Ra (AI agent) handed off to Sam",
                reasonPrefix: "Reason:",
                reason: "Needs approval",
                time: "3 minutes ago"
            ) == "Handoff. Ra (AI agent) handed off to Sam. Reason: Needs approval. 3 minutes ago"
        )
        #expect(
            MonarchAiHandoffTokens.accessibilityLabel(
                label: "Handoff",
                sentence: "Ra handed off to Sam",
                reasonPrefix: "Reason:",
                reason: "",
                time: nil
            ) == "Handoff. Ra handed off to Sam"
        )
        #expect(
            MonarchAiHandoffTokens.accessibilityLabel(
                label: "",
                sentence: "Ra handed off to Sam",
                reasonPrefix: "",
                reason: "Done",
                time: nil
            ) == "Ra handed off to Sam. Done"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiHandoffStrings.label == "Handoff")
        #expect(MonarchAiHandoffStrings.agentKind == "AI agent")
        #expect(MonarchAiHandoffStrings.reason == "Reason:")
        #expect(MonarchAiHandoffStrings.handedOff("A", "B") == "A handed off to B")
        #expect(MonarchAiHandoffStrings.agentName("A", kind: "AI agent") == "A (AI agent)")
    }
}
