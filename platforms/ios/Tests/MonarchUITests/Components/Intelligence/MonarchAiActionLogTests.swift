import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
private let research = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
private let scheduler = MonarchAiAgentRef(id: "scheduler", name: "Scheduler")

private let entries = [
    MonarchAiActionLogEntry(
        id: "summary",
        date: now.addingTimeInterval(-240),
        agent: research,
        action: "added a summary to",
        target: "Quarterly report",
        url: URL(string: "https://example.com/report"),
        isUndoable: true
    ),
    MonarchAiActionLogEntry(
        id: "rename",
        date: now.addingTimeInterval(-7_200),
        agent: research,
        action: "renamed",
        target: "Roadmap",
        isUndoable: true,
        isUndone: true
    ),
    MonarchAiActionLogEntry(
        id: "meeting",
        date: now.addingTimeInterval(-86_400 * 2),
        agent: scheduler,
        action: "moved the weekly planning meeting with the whole product and design team to Thursday afternoon"
    ),
]

enum MonarchAiActionLogFixtures: ComponentFixtures {
    static let id = "ai-action-log"
    static let fixtures = [
        Fixture("undoable, undone and long") {
            MonarchAiActionLog(entries, now: now, onUndo: { _ in })
        },
        Fixture("read only") {
            MonarchAiActionLog(entries, now: now)
        },
        Fixture("paged, show more") {
            MonarchAiActionLog(entries, now: now, pageSize: 1, onUndo: { _ in })
        },
        Fixture("empty") {
            MonarchAiActionLog([])
        },
        Fixture("custom labels") {
            MonarchAiActionLog(
                [entries[1]],
                now: now,
                labels: MonarchAiActionLogLabels(actorKind: "Bot", undone: "Reverted")
            )
        },
    ]
}

@Suite("MonarchAiActionLog")
struct MonarchAiActionLogTests {
    private typealias Tokens = MonarchAiActionLogTokens
    private let labels = MonarchAiActionLogLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("Paging shows a step at a time and counts what is left")
    func paging() {
        #expect(Tokens.step(0) == 1)
        #expect(Tokens.step(-5) == 1)
        #expect(Tokens.step(20) == 20)
        #expect(Tokens.hiddenCount(total: 3, shown: 1) == 2)
        #expect(Tokens.hiddenCount(total: 3, shown: 5) == 0)
        #expect(Tokens.moreCount(total: 10, shown: 2, step: 2) == 2)
        #expect(Tokens.moreCount(total: 3, shown: 2, step: 2) == 1)
        #expect(Tokens.moreCount(total: 2, shown: 2, step: 2) == 0)
    }

    @Test("An entry is described as agent, action and target")
    func describe() {
        #expect(Tokens.describe(entries[0]) == "Research agent added a summary to Quarterly report")
        #expect(Tokens.describe(entries[2]).hasPrefix("Scheduler moved"))
        let blankTarget = MonarchAiActionLogEntry(id: "b", date: now, agent: scheduler, action: "archived", target: " ")
        #expect(Tokens.describe(blankTarget) == "Scheduler archived")
    }

    @Test("Undo shows only for undoable entries with a handler that are not undone")
    func canUndo() {
        #expect(Tokens.canUndo(entries[0], state: nil, hasHandler: true))
        #expect(Tokens.canUndo(entries[0], state: .pending, hasHandler: true))
        #expect(Tokens.canUndo(entries[0], state: .failed, hasHandler: true))
        #expect(!Tokens.canUndo(entries[0], state: .done, hasHandler: true))
        #expect(!Tokens.canUndo(entries[0], state: nil, hasHandler: false))
        #expect(!Tokens.canUndo(entries[1], state: nil, hasHandler: true))
        #expect(!Tokens.canUndo(entries[2], state: nil, hasHandler: true))
        #expect(Tokens.isUndone(entries[1], state: nil))
        #expect(Tokens.isUndone(entries[0], state: .done))
        #expect(!Tokens.isUndone(entries[0], state: .failed))
    }

    @Test("Only http and https targets are links")
    func safeURL() {
        #expect(Tokens.safeURL(URL(string: "https://example.com")) != nil)
        #expect(Tokens.safeURL(URL(string: "HTTP://example.com")) != nil)
        #expect(Tokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(Tokens.safeURL(URL(string: "file:///etc/hosts")) == nil)
        #expect(Tokens.safeURL(nil) == nil)
    }

    @Test("The meta line is the time, then Undone")
    func meta() {
        let time = Tokens.meta(entries[0], isUndone: false, now: now, locale: locale, labels: labels)
        #expect(time == "4 minutes ago")
        let undone = Tokens.meta(entries[1], isUndone: true, now: now, locale: locale, labels: labels)
        #expect(undone == "2 hours ago · Undone")
        let custom = MonarchAiActionLogLabels(undone: "Reverted")
        #expect(
            Tokens.meta(entries[1], isUndone: true, now: now, locale: locale, labels: custom)
                == "2 hours ago · Reverted"
        )
    }

    @Test("VoiceOver reads the agent as an AI agent, then the action")
    func accessibility() {
        #expect(
            Tokens.accessibilityLabel(entries[0], labels: labels)
                == "Research agent (AI agent) added a summary to Quarterly report"
        )
        #expect(
            Tokens.accessibilityLabel(entries[0], labels: MonarchAiActionLogLabels(actorKind: "Bot")).contains("(Bot)")
        )
        #expect(Tokens.accessibilityValue(meta: "4 minutes ago", failure: nil) == "4 minutes ago")
        #expect(
            Tokens.accessibilityValue(meta: "4 minutes ago", failure: "Could not undo")
                == "4 minutes ago, Could not undo"
        )
    }

    @Test("Undone text is muted; active text is the default colour")
    func textRole() {
        #expect(Tokens.textRole(isUndone: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.textRole(isUndone: false) == \MonarchColorTokens.textDefault)
    }

    @Test("Undo strings name the entry and can be replaced")
    func undoStrings() {
        let entry = entries[0]
        #expect(Tokens.undoLabel(entry, labels: labels) == "Undo: Research agent added a summary to Quarterly report")
        #expect(Tokens.undoneAnnouncement(entry, labels: labels).hasPrefix("Undone: Research agent"))
        #expect(Tokens.undoFailed(entry, labels: labels).hasPrefix("Could not undo: Research agent"))
        let custom = MonarchAiActionLogLabels(undoLabel: { "Revert \($0)" })
        #expect(Tokens.undoLabel(entry, labels: custom) == "Revert Research agent added a summary to Quarterly report")
    }

    @Test("Show more says how many it adds, singular and plural")
    func showMore() {
        #expect(Tokens.showMore(1, labels: labels) == "Show 1 more action")
        #expect(Tokens.showMore(3, labels: labels) == "Show 3 more actions")
        #expect(Tokens.showMore(2, labels: MonarchAiActionLogLabels(showMore: { "+\($0)" })) == "+2")
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiActionLogStrings.label == "AI activity")
        #expect(MonarchAiActionLogStrings.openTarget("Roadmap") == "Open Roadmap")
        #expect(Tokens.actorKind(labels) == "AI agent")
        for value in [
            MonarchAiActionLogStrings.undo, MonarchAiActionLogStrings.undone, MonarchAiActionLogStrings.empty,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
