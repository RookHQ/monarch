import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let journeyNow = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let patentStages = [
    MonarchStageJourneyStage(id: "draft", label: "Draft", group: "Preparation"),
    MonarchStageJourneyStage(id: "filed", label: "Filed", group: "Preparation"),
    MonarchStageJourneyStage(
        id: "exam",
        label: "Examination",
        group: "Prosecution",
        description: "The office reviews the claims. Answer office actions before their deadlines."
    ),
    MonarchStageJourneyStage(id: "allowed", label: "Allowed", group: "Prosecution"),
    MonarchStageJourneyStage(id: "granted", label: "Granted", group: "Outcome"),
]

private let patentMoves = [
    MonarchStageJourneyTransition(id: "allowed", label: "Allowed", needsInput: true),
    MonarchStageJourneyTransition(id: "granted", label: "Granted", isDisabled: true),
    MonarchStageJourneyTransition(id: "withdrawn", label: "Withdrawn", kind: .exit),
    MonarchStageJourneyTransition(id: "filed", label: "Filed", kind: .back),
]

enum MonarchStageJourneyFixtures: ComponentFixtures {
    static let id = "stage-journey"

    static let fixtures = [
        Fixture("full, with moves, meta and time in stage") {
            MonarchStageJourney(
                patentStages,
                currentID: "exam",
                enteredAt: journeyNow.addingTimeInterval(-3 * 86400),
                currentMeta: "Office action due",
                transitions: patentMoves,
                now: journeyNow,
                onTransition: { _ in }
            )
        },
        Fixture("ended in success, with extra content") {
            MonarchStageJourney(
                patentStages,
                currentID: "granted",
                terminal: .success,
                now: journeyNow,
                content: {
                    Text("Renewal is due in four years.")
                        .monarchTextStyle(.caption)
                }
            )
        },
        Fixture("closed, without groups") {
            MonarchStageJourney(
                [
                    MonarchStageJourneyStage(id: "lead", label: "Lead"),
                    MonarchStageJourneyStage(id: "lost", label: "Lost"),
                ],
                currentID: "lost",
                terminal: .closed,
                enteredAt: Date(timeIntervalSinceNow: -7200)
            )
        },
        Fixture("strip only, and an unknown current stage") {
            VStack(alignment: .leading) {
                MonarchStageJourney(patentStages, currentID: "filed", variant: .strip)
                MonarchStageJourney(patentStages, currentID: "missing")
            }
        },
        Fixture("moves without an action are hidden; custom strings") {
            MonarchStageJourney(
                patentStages,
                currentID: "draft",
                transitions: patentMoves,
                labels: MonarchStageJourneyLabels(now: { "Aktuell: \($0)" })
            )
        },
        Fixture("history, newest first") {
            MonarchStageHistory(
                [
                    MonarchStageHistoryEntry(
                        id: "3",
                        from: "Examination",
                        to: "Filed",
                        at: journeyNow.addingTimeInterval(-3600),
                        actor: "Alex Morgan",
                        note: "The claims need another review before we answer the office action.",
                        isBackward: true
                    ),
                    MonarchStageHistoryEntry(
                        id: "2",
                        from: "Filed",
                        to: "Examination",
                        at: journeyNow.addingTimeInterval(-2 * 86400),
                        actor: "Automation",
                        meta: "2 deadlines added"
                    ),
                    MonarchStageHistoryEntry(id: "1", to: "Filed", at: journeyNow.addingTimeInterval(-9 * 86400)),
                ],
                now: journeyNow
            )
        },
        Fixture("history, empty and live") {
            VStack(alignment: .leading) {
                MonarchStageHistory([])
                MonarchStageHistory([], emptyMessage: "Nothing has moved.")
                MonarchStageHistory([MonarchStageHistoryEntry(id: "1", to: "Lead", at: Date(timeIntervalSinceNow: -60))]
                )
            }
        },
    ]
}

private struct MoveFailure: LocalizedError {
    var errorDescription: String? { "Answer the office action first." }
}

private struct SilentFailure: Error {}

@Suite("MonarchStageJourney")
struct MonarchStageJourneyTests {
    typealias Tokens = MonarchStageJourneyTokens

    private let labels = MonarchStageJourneyLabels()
    private let historyLabels = MonarchStageHistoryLabels()
    private let english = Locale(identifier: "en_US")

    // MARK: Stages

    @Test("Stages before the current one are done and after it upcoming")
    func statuses() {
        let current = Tokens.currentIndex(of: patentStages, currentID: "exam")
        #expect(current == 2)
        #expect(Tokens.status(at: 0, currentIndex: current) == .done)
        #expect(Tokens.status(at: 2, currentIndex: current) == .current)
        #expect(Tokens.status(at: 4, currentIndex: current) == .upcoming)
        #expect(Tokens.currentIndex(of: patentStages, currentID: "missing") == nil)
        #expect(Tokens.status(at: 0, currentIndex: nil) == .upcoming)
    }

    @Test("Consecutive stages with the same group are grouped, in order")
    func grouping() {
        let groups = Tokens.groups(patentStages)
        #expect(groups.map(\.name) == ["Preparation", "Prosecution", "Outcome"])
        #expect(groups.map(\.indices) == [[0, 1], [2, 3], [4]])
        let split = Tokens.groups([
            MonarchStageJourneyStage(id: "a", label: "A", group: "X"),
            MonarchStageJourneyStage(id: "b", label: "B"),
            MonarchStageJourneyStage(id: "c", label: "C", group: "X"),
        ])
        #expect(split.map(\.name) == ["X", nil, "X"])
        #expect(Tokens.groups([]).isEmpty)
    }

    @Test("Stage colours follow the status and the outcome")
    func stageColours() {
        let done = Tokens.stageAppearance(status: .done, terminal: nil)
        #expect(done.background == \MonarchColorTokens.accentSubtle)
        let current = Tokens.stageAppearance(status: .current, terminal: nil)
        #expect(current.background == \MonarchColorTokens.accentSolid)
        #expect(current.foreground == \MonarchColorTokens.accentOnSolid)
        let success = Tokens.stageAppearance(status: .current, terminal: .success)
        #expect(success.background == \MonarchColorTokens.successSolid)
        #expect(success.foreground == \MonarchColorTokens.successOnSolid)
        let closed = Tokens.stageAppearance(status: .current, terminal: .closed)
        #expect(closed.background == \MonarchColorTokens.bgEmphasis)
        let upcoming = Tokens.stageAppearance(status: .upcoming, terminal: .success)
        #expect(upcoming.background == nil)
        #expect(upcoming.foreground == \MonarchColorTokens.textMuted)
    }

    @Test("A done stage has a checkmark and an ended stage its outcome, so status is not colour alone")
    func stageSymbols() {
        #expect(Tokens.stageSymbol(status: .done, terminal: nil) == "checkmark")
        #expect(Tokens.stageSymbol(status: .current, terminal: nil) == nil)
        #expect(Tokens.stageSymbol(status: .current, terminal: .success) == "checkmark.circle.fill")
        #expect(Tokens.stageSymbol(status: .current, terminal: .closed) == "xmark.circle")
        #expect(Tokens.stageSymbol(status: .upcoming, terminal: nil) == nil)
        #expect(Tokens.stageWeight(status: .current) == .medium)
        #expect(Tokens.stageWeight(status: .done) == .regular)
    }

    @Test("The card is tinted for success and subtle when closed")
    func cardColours() {
        #expect(Tokens.cardAppearance(terminal: nil).background == \MonarchColorTokens.bgSurface)
        #expect(Tokens.cardAppearance(terminal: .success).background == \MonarchColorTokens.successSubtle)
        #expect(Tokens.cardAppearance(terminal: .success).border == \MonarchColorTokens.successBorder)
        #expect(Tokens.cardAppearance(terminal: .closed).background == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.outcomeForeground(.success) == \MonarchColorTokens.successStrong)
        #expect(Tokens.outcomeForeground(.closed) == \MonarchColorTokens.textMuted)
    }

    @Test("Relative times are words from the reference time")
    func relativeTime() {
        let text = Tokens.relativeText(journeyNow.addingTimeInterval(-3 * 86400), now: journeyNow, locale: english)
        #expect(text == "3 days ago")
        #expect(MonarchStageJourneyStrings.since(text, labels) == "since 3 days ago")
    }

    // MARK: Moves

    @Test("Moves are next steps, then exits, then one step back")
    func order() {
        let moves = patentMoves.reversed() + [MonarchStageJourneyTransition(id: "draft", label: "Draft", kind: .back)]
        let ordered = Tokens.orderedMoves(Array(moves))
        #expect(ordered.map(\.id) == ["granted", "allowed", "withdrawn", "filed"])
    }

    @Test("The first next step is primary, others secondary, exits and back ghost")
    func variants() {
        #expect(Tokens.buttonVariant(for: patentMoves[0], in: patentMoves) == .primary)
        #expect(Tokens.buttonVariant(for: patentMoves[1], in: patentMoves) == .secondary)
        #expect(Tokens.buttonVariant(for: patentMoves[2], in: patentMoves) == .ghost)
        #expect(Tokens.buttonVariant(for: patentMoves[3], in: patentMoves) == .ghost)
    }

    @Test("A step back is titled Back to and has the undo symbol")
    func backTitle() {
        #expect(Tokens.buttonTitle(for: patentMoves[3], labels: labels) == "Back to Filed")
        #expect(Tokens.buttonTitle(for: patentMoves[0], labels: labels) == "Allowed")
        #expect(Tokens.buttonSymbol(for: patentMoves[3]) == "arrow.uturn.backward")
        #expect(Tokens.buttonSymbol(for: patentMoves[0]) == nil)
    }

    @Test("While a move runs the others are disabled; a disabled move stays disabled")
    func enabled() {
        #expect(Tokens.isEnabled(patentMoves[0], busyID: nil))
        #expect(Tokens.isEnabled(patentMoves[0], busyID: "allowed"))
        #expect(!Tokens.isEnabled(patentMoves[2], busyID: "allowed"))
        #expect(!Tokens.isEnabled(patentMoves[1], busyID: nil))
    }

    @Test("A failed move shows the error's description, or the built-in message")
    func failures() {
        #expect(Tokens.failureMessage(for: MoveFailure(), labels: labels) == "Answer the office action first.")
        #expect(
            Tokens.failureMessage(for: SilentFailure(), labels: labels) == "The move did not go through. Try again."
        )
    }

    // MARK: Accessibility

    @Test("Stages are read with their status and the outcome")
    func stageValues() {
        #expect(Tokens.stageAccessibilityValue(status: .done, terminal: nil, labels: labels) == "done")
        #expect(Tokens.stageAccessibilityValue(status: .current, terminal: nil, labels: labels) == "current stage")
        #expect(
            Tokens.stageAccessibilityValue(status: .current, terminal: .success, labels: labels)
                == "current stage, Completed"
        )
        #expect(Tokens.stageAccessibilityValue(status: .upcoming, terminal: nil, labels: labels).isEmpty)
    }

    @Test("Moves are read as Move to, with a note when they ask for details")
    func moveLabels() {
        #expect(Tokens.moveAccessibilityLabel(patentMoves[0], labels: labels) == "Move to Allowed, asks for details")
        #expect(Tokens.moveAccessibilityLabel(patentMoves[2], labels: labels) == "Move to Withdrawn")
        #expect(Tokens.moveAccessibilityLabel(patentMoves[3], labels: labels) == "Back to Filed")
        let custom = MonarchStageJourneyLabels(moveTo: { "Nach \($0)" })
        #expect(Tokens.moveAccessibilityLabel(patentMoves[2], labels: custom) == "Nach Withdrawn")
    }

    @Test("The card title is read with the outcome")
    func title() {
        #expect(Tokens.titleAccessibilityLabel(stage: "Granted", terminal: nil, labels: labels) == "Now: Granted")
        #expect(
            Tokens.titleAccessibilityLabel(stage: "Lost", terminal: .closed, labels: labels) == "Now: Lost, Closed"
        )
    }

    // MARK: History

    @Test("History entries are read as words, with moved back, time, actor, facts and note")
    func historyAccessibility() {
        let back = MonarchStageHistoryEntry(
            id: "1",
            from: "Examination",
            to: "Filed",
            at: journeyNow,
            actor: "Alex",
            meta: "1 deadline removed",
            note: "Claims need work",
            isBackward: true
        )
        #expect(
            Tokens.historyAccessibilityLabel(back, time: "1 hour ago", labels: historyLabels)
                == "Examination to Filed, (moved back), 1 hour ago, Alex, 1 deadline removed, Claims need work"
        )
        let first = MonarchStageHistoryEntry(id: "0", to: "Filed", at: journeyNow)
        #expect(Tokens.historyAccessibilityLabel(first, time: "now", labels: historyLabels) == "Started at Filed, now")
        #expect(Tokens.historyMetaText(back, time: "1 hour ago") == "1 hour ago · Alex · 1 deadline removed")
        #expect(Tokens.historyMetaText(first, time: "now") == "now")
        #expect(Tokens.historyDot(isBackward: true) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.historyDot(isBackward: false) == \MonarchColorTokens.accentSolid)
    }

    @Test("Every built-in string resolves")
    func strings() {
        let journey = [
            MonarchStageJourneyStrings.stages(labels),
            MonarchStageJourneyStrings.now("X", labels),
            MonarchStageJourneyStrings.since("X", labels),
            MonarchStageJourneyStrings.done(labels),
            MonarchStageJourneyStrings.current(labels),
            MonarchStageJourneyStrings.success(labels),
            MonarchStageJourneyStrings.closed(labels),
            MonarchStageJourneyStrings.nextSteps(labels),
            MonarchStageJourneyStrings.moveTo("X", labels),
            MonarchStageJourneyStrings.backTo("X", labels),
            MonarchStageJourneyStrings.needsInput(labels),
            MonarchStageJourneyStrings.moved("X", labels),
            MonarchStageJourneyStrings.failed(labels),
            MonarchStageHistoryStrings.started("X", historyLabels),
            MonarchStageHistoryStrings.to(historyLabels),
            MonarchStageHistoryStrings.movedBack(historyLabels),
            MonarchStageHistoryStrings.empty(historyLabels),
        ]
        for string in journey {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchStageJourneyStrings.moved("Granted", labels) == "Moved to Granted")
        #expect(MonarchStageHistoryStrings.empty(MonarchStageHistoryLabels(empty: "Leer")) == "Leer")
    }
}
