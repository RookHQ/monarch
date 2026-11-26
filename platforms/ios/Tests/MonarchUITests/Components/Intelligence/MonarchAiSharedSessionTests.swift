import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiSharedSessionFixtures: ComponentFixtures {
    static let id = "ai-shared-session"
    static let participants = [
        MonarchAiSharedSessionParticipant(id: "sam", name: "Sam Lee", activity: .typing),
        MonarchAiSharedSessionParticipant(id: "maya", name: "Maya", role: .owner, isYou: true),
        MonarchAiSharedSessionParticipant(id: "ra", name: "Research agent", kind: .agent),
        MonarchAiSharedSessionParticipant(id: "jo", name: "Jo", role: .viewer, presence: .idle, activity: .reviewing),
        MonarchAiSharedSessionParticipant(id: "kim", name: "Kim", presence: .away),
    ]
    static let queue = [
        MonarchAiSharedSessionRequest(id: "r1", participantID: "sam", preview: "Compare the Q3 numbers with last year"),
        MonarchAiSharedSessionRequest(id: "r2", participantID: "maya", preview: "Draft the summary for the board"),
    ]
    static let answering = MonarchAiSharedSessionAnswering(
        participantID: "ra",
        forParticipantID: "sam",
        label: "Summarise the notes"
    )

    static let fixtures = [
        Fixture("owner with queue open and every action") {
            MonarchAiSharedSession(
                participants,
                answering: answering,
                queue: queue,
                maxVisible: 3,
                isQueueExpanded: .constant(true),
                onWithdraw: { _ in },
                onReorder: { _, _ in },
                onRemove: { _ in },
                onInvite: {}
            )
        },
        Fixture("quiet, no queue") {
            MonarchAiSharedSession([
                MonarchAiSharedSessionParticipant(id: "maya", name: "Maya", isYou: true)
            ])
        },
        Fixture("viewer, access requested and not") {
            VStack {
                MonarchAiSharedSession(
                    [MonarchAiSharedSessionParticipant(id: "v", name: "Viewer", role: .viewer, isYou: true)],
                    onRequestAccess: {}
                )
                MonarchAiSharedSession(
                    [MonarchAiSharedSessionParticipant(id: "v", name: "Viewer", role: .viewer, isYou: true)],
                    isAccessRequested: true
                )
            }
        },
        Fixture("participant list") {
            MonarchAiSharedSessionList(
                participants: participants,
                answering: answering,
                answeringText: "Research agent is answering Sam Lee’s request",
                following: .constant(["sam"]),
                labels: MonarchAiSharedSessionLabels(),
                onDone: {}
            )
        },
        Fixture("empty") {
            MonarchAiSharedSession([])
        },
    ]
}

@Suite("MonarchAiSharedSession")
struct MonarchAiSharedSessionTests {
    private let labels = MonarchAiSharedSessionLabels()
    private let locale = Locale(identifier: "en_US")
    private let participants = MonarchAiSharedSessionFixtures.participants

    @Test("People come first with you at the top, then agents")
    func groups() {
        let groups = MonarchAiSharedSessionTokens.groups(participants)
        #expect(groups.people.map(\.id) == ["maya", "sam", "jo", "kim"])
        #expect(groups.agents.map(\.id) == ["ra"])
        #expect(groups.you?.id == "maya")
        #expect(MonarchAiSharedSessionTokens.yourRole(participants) == .owner)
        #expect(MonarchAiSharedSessionTokens.yourRole([participants[0]]) == nil)
    }

    @Test("A participant is read with role or kind, presence and activity")
    func participantText() {
        #expect(
            MonarchAiSharedSessionTokens.participantText(participants[1], labels: labels) == "Maya (you), Owner, Active"
        )
        #expect(
            MonarchAiSharedSessionTokens.participantText(participants[2], labels: labels)
                == "Research agent, AI agent, Active"
        )
        #expect(
            MonarchAiSharedSessionTokens.participantText(participants[3], labels: labels)
                == "Jo, Viewer, Idle, Reviewing"
        )
    }

    @Test("Presence maps to the avatar's dot")
    func presence() {
        #expect(MonarchAiSharedSessionTokens.presenceStatus(.active) == .online)
        #expect(MonarchAiSharedSessionTokens.presenceStatus(.idle) == .away)
        #expect(MonarchAiSharedSessionTokens.presenceStatus(.away) == .offline)
    }

    @Test("The status line says who is answered, what others do and what waits")
    func status() {
        let status = MonarchAiSharedSessionTokens.status(
            participants: participants,
            answering: MonarchAiSharedSessionFixtures.answering,
            queueCount: 2,
            locale: locale,
            labels: labels
        )
        #expect(
            status.text
                == "Research agent is answering Sam Lee’s request: Summarise the notes · Sam Lee is typing… · Jo is reviewing · 2 requests waiting"
        )
        // Typing is shown but not announced.
        #expect(
            status.announcement
                == "Research agent is answering Sam Lee’s request: Summarise the notes. Jo is reviewing. 2 requests waiting"
        )
    }

    @Test("Your own request and your own activity read differently")
    func statusYou() {
        var people = participants
        people[1].activity = .typing
        let status = MonarchAiSharedSessionTokens.status(
            participants: people,
            answering: MonarchAiSharedSessionAnswering(participantID: "missing", forParticipantID: "maya"),
            queueCount: 1,
            locale: locale,
            labels: labels
        )
        #expect(
            status.text
                == "Assistant is answering your request · Sam Lee is typing… · Jo is reviewing · 1 request waiting"
        )
        let quiet = MonarchAiSharedSessionTokens.status(
            participants: [],
            answering: nil,
            queueCount: 0,
            locale: locale,
            labels: labels
        )
        #expect(quiet.parts.isEmpty && quiet.text.isEmpty && quiet.announcement.isEmpty)
    }

    @Test("Two names are listed; more than two are counted")
    func names() {
        let typists = ["a", "b", "c"].map {
            MonarchAiSharedSessionParticipant(id: $0, name: $0.uppercased(), activity: .typing)
        }
        let two = MonarchAiSharedSessionTokens.status(
            participants: Array(typists.prefix(2)),
            answering: nil,
            queueCount: 0,
            locale: locale,
            labels: labels
        )
        #expect(two.text == "A and B are typing…")
        let three = MonarchAiSharedSessionTokens.status(
            participants: typists,
            answering: nil,
            queueCount: 0,
            locale: locale,
            labels: labels
        )
        #expect(three.text == "3 people are typing…")
    }

    @Test("Only a changed, non-empty status after the first is announced")
    func announcementChange() {
        #expect(MonarchAiSharedSessionTokens.announcementChange(previous: nil, current: "x") == nil)
        #expect(MonarchAiSharedSessionTokens.announcementChange(previous: "x", current: "x") == nil)
        #expect(MonarchAiSharedSessionTokens.announcementChange(previous: "x", current: "") == nil)
        #expect(MonarchAiSharedSessionTokens.announcementChange(previous: "x", current: "y") == "y")
    }

    @Test("Queue buttons follow your role and whose request it is")
    func queueActions() {
        let queue = MonarchAiSharedSessionFixtures.queue
        func actions(_ index: Int, role: MonarchAiSharedSessionRole?) -> [MonarchAiSharedSessionTokens.QueueAction] {
            MonarchAiSharedSessionTokens.queueActions(
                queue[index],
                index: index,
                count: queue.count,
                youID: "maya",
                role: role,
                canReorder: true,
                canWithdraw: true,
                canRemove: true
            )
        }
        #expect(actions(0, role: .owner) == [.moveUp(isDisabled: true), .moveDown(isDisabled: false), .remove])
        #expect(actions(1, role: .owner) == [.moveUp(isDisabled: false), .moveDown(isDisabled: true), .withdraw])
        #expect(actions(0, role: .editor).isEmpty)
        #expect(actions(1, role: .editor) == [.withdraw])
        #expect(
            MonarchAiSharedSessionTokens.queueActions(
                queue[1],
                index: 1,
                count: 2,
                youID: "maya",
                role: .owner,
                canReorder: false,
                canWithdraw: false,
                canRemove: true
            ) == [.remove]
        )
    }

    @Test("A move gives the new order")
    func reorder() {
        #expect(MonarchAiSharedSessionTokens.reordered(["a", "b", "c"], from: 2, to: 0) == ["c", "a", "b"])
        #expect(MonarchAiSharedSessionTokens.reordered(["a", "b", "c"], from: 0, to: 1) == ["b", "a", "c"])
        #expect(MonarchAiSharedSessionTokens.reordered(["a", "b"], from: 0, to: -1) == nil)
        #expect(MonarchAiSharedSessionTokens.reordered(["a", "b"], from: 1, to: 1) == nil)
    }

    @Test("Every built-in string can be replaced")
    func overrides() {
        let custom = MonarchAiSharedSessionLabels(
            nameWithYou: { "\($0) (tú)" },
            role: { _ in "Rol" },
            activityLine: { _, names, _ in "\(names) …" },
            queueCount: { "\($0) en cola" }
        )
        #expect(
            MonarchAiSharedSessionTokens.participantText(participants[1], labels: custom) == "Maya (tú), Rol, Active"
        )
        let status = MonarchAiSharedSessionTokens.status(
            participants: [participants[0]],
            answering: nil,
            queueCount: 3,
            locale: locale,
            labels: custom
        )
        #expect(status.text == "Sam Lee … · 3 en cola")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiSharedSessionStrings.label == "Shared session")
        #expect(MonarchAiSharedSessionStrings.here(people: 3, agents: 1) == "3 people and 1 agent here")
        #expect(MonarchAiSharedSessionStrings.here(people: 1, agents: 0) == "1 person here")
        #expect(MonarchAiSharedSessionStrings.here(people: 0, agents: 2) == "2 agents here")
        #expect(MonarchAiSharedSessionStrings.showAll(4) == "Shows all 4 participants.")
        #expect(MonarchAiSharedSessionStrings.queueToggle(2) == "Queue (2)")
        #expect(
            MonarchAiSharedSessionStrings.requestAction("Move up", requester: "Sam", position: 2)
                == "Move up: request 2, from Sam"
        )
        #expect(
            MonarchAiSharedSessionStrings.moved("Sam", position: 1, total: 3)
                == "Sam’s request moved to position 1 of 3"
        )
        #expect(MonarchAiSharedSessionStrings.followLabel("Maya") == "Follow Maya")
        #expect(MonarchAiSharedSessionStrings.viewerNotice == "You can watch but not send")
        #expect(MonarchAiSharedSessionStrings.quiet == "No requests in progress")
    }
}
