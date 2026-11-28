import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiChainOfStepsFixtures: ComponentFixtures {
    static let id = "ai-chain-of-steps"
    static let running = [
        MonarchAiPlanStep(id: "read", label: "Read the attached contract", state: .done),
        MonarchAiPlanStep(id: "search", label: "Search past invoices", state: .active),
        MonarchAiPlanStep(id: "draft", label: "Draft the summary"),
    ]
    static let finished = [
        MonarchAiPlanStep(id: "a", label: "Look up the customer", state: .done),
        MonarchAiPlanStep(id: "b", label: "Check the payment", state: .failed),
        MonarchAiPlanStep(id: "c", label: "Email the account owner", state: .skipped),
    ]

    static let fixtures = [
        Fixture("running, with details") {
            MonarchAiChainOfSteps(
                running,
                details: ["read": "14 pages", "search": "Found 12 so far"],
                announces: false
            )
        },
        Fixture("finished, folded") {
            MonarchAiChainOfSteps(finished, announces: false)
        },
        Fixture("finished, open, every final state") {
            MonarchAiChainOfSteps(
                finished,
                details: ["b": "The payment service did not answer."],
                startsExpanded: true,
                announces: false
            )
        },
        Fixture("finished, not collapsible") {
            MonarchAiChainOfSteps(finished, isCollapsible: false, announces: false)
        },
        Fixture("long label") {
            MonarchAiChainOfSteps(
                [
                    MonarchAiPlanStep(
                        id: "long",
                        label:
                            "Compare every clause of the supplier agreement with the standard terms and list the differences",
                        state: .active
                    )
                ],
                announces: false
            )
        },
        Fixture("not started") {
            MonarchAiChainOfSteps([MonarchAiPlanStep(id: "x", label: "Plan the trip")], announces: false)
        },
    ]
}

@Suite("MonarchAiChainOfSteps")
struct MonarchAiChainOfStepsTests {
    private let labels = MonarchAiChainOfStepsLabels()

    @Test("Done, failed and skipped are final; the list folds only when all are")
    func folding() {
        #expect(!MonarchAiChainOfStepsTokens.isFinished(.todo))
        #expect(!MonarchAiChainOfStepsTokens.isFinished(.active))
        #expect(MonarchAiChainOfStepsTokens.isFinished(.done))
        #expect(MonarchAiChainOfStepsTokens.isFinished(.failed))
        #expect(MonarchAiChainOfStepsTokens.isFinished(.skipped))
        #expect(MonarchAiChainOfStepsTokens.isFolded(MonarchAiChainOfStepsFixtures.finished, isCollapsible: true))
        #expect(!MonarchAiChainOfStepsTokens.isFolded(MonarchAiChainOfStepsFixtures.finished, isCollapsible: false))
        #expect(!MonarchAiChainOfStepsTokens.isFolded(MonarchAiChainOfStepsFixtures.running, isCollapsible: true))
        #expect(!MonarchAiChainOfStepsTokens.isFolded([], isCollapsible: true))
    }

    @Test("The summary counts steps and failures")
    func summary() {
        #expect(
            MonarchAiChainOfStepsTokens.summary(MonarchAiChainOfStepsFixtures.finished, labels: labels)
                == "3 steps, 1 failed"
        )
        let one = [MonarchAiPlanStep(id: "a", label: "A", state: .done)]
        #expect(MonarchAiChainOfStepsTokens.summary(one, labels: labels) == "1 step")
        let oneFailed = [MonarchAiPlanStep(id: "a", label: "A", state: .failed)]
        #expect(MonarchAiChainOfStepsTokens.summary(oneFailed, labels: labels) == "1 step, 1 failed")
        let two = [
            MonarchAiPlanStep(id: "a", label: "A", state: .done), MonarchAiPlanStep(id: "b", label: "B", state: .done),
        ]
        #expect(MonarchAiChainOfStepsTokens.summary(two, labels: labels) == "2 steps")
        let custom = MonarchAiChainOfStepsLabels(summary: { count, _ in "\(count) étapes" })
        #expect(MonarchAiChainOfStepsTokens.summary(two, labels: custom) == "2 étapes")
    }

    @Test("Nothing is announced for the first state")
    func noHistory() {
        #expect(
            MonarchAiChainOfStepsTokens.announcements(
                previous: nil,
                steps: MonarchAiChainOfStepsFixtures.running,
                labels: labels
            ).isEmpty
        )
    }

    @Test("A start, a failure and the end are announced once")
    func announcements() {
        let before = MonarchAiChainOfStepsTokens.snapshot(MonarchAiChainOfStepsFixtures.running)
        let next = [
            MonarchAiPlanStep(id: "read", label: "Read the attached contract", state: .done),
            MonarchAiPlanStep(id: "search", label: "Search past invoices", state: .failed),
            MonarchAiPlanStep(id: "draft", label: "Draft the summary", state: .active),
        ]
        #expect(
            MonarchAiChainOfStepsTokens.announcements(previous: before, steps: next, labels: labels) == [
                "Step failed: Search past invoices", "Step 3 of 3: Draft the summary",
            ]
        )
        let done = [
            MonarchAiPlanStep(id: "read", label: "Read the attached contract", state: .done),
            MonarchAiPlanStep(id: "search", label: "Search past invoices", state: .failed),
            MonarchAiPlanStep(id: "draft", label: "Draft the summary", state: .done),
        ]
        let afterNext = MonarchAiChainOfStepsTokens.snapshot(next)
        #expect(
            MonarchAiChainOfStepsTokens.announcements(previous: afterNext, steps: done, labels: labels) == [
                "Finished 3 steps, 1 failed"
            ]
        )
        // Already finished: no second completion.
        let afterDone = MonarchAiChainOfStepsTokens.snapshot(done)
        #expect(MonarchAiChainOfStepsTokens.announcements(previous: afterDone, steps: done, labels: labels).isEmpty)
        let allDone = done.map { MonarchAiPlanStep(id: $0.id, label: $0.label, state: .done) }
        #expect(
            MonarchAiChainOfStepsTokens.announcements(previous: afterNext, steps: allDone, labels: labels) == [
                "Finished 3 steps"
            ]
        )
    }

    @Test("State is written for failed and skipped, and read for every step")
    func stateText() {
        #expect(MonarchAiChainOfStepsTokens.showsStateText(.failed))
        #expect(MonarchAiChainOfStepsTokens.showsStateText(.skipped))
        #expect(!MonarchAiChainOfStepsTokens.showsStateText(.done))
        #expect(!MonarchAiChainOfStepsTokens.showsStateText(.active))
        let names = MonarchAiPlanStepState.allCases.map { MonarchAiChainOfStepsTokens.stateName($0, labels: labels) }
        #expect(names == ["Not started", "In progress", "Done", "Skipped", "Failed"])
        #expect(
            MonarchAiChainOfStepsTokens.accessibilityValue(stateName: "Failed", detail: "Timed out")
                == "Failed, Timed out"
        )
        #expect(MonarchAiChainOfStepsTokens.accessibilityValue(stateName: "Done", detail: "") == "Done")
        #expect(MonarchAiChainOfStepsStrings.label == "Steps")
    }

    @Test("Every state has its own icon; colours follow the state")
    func appearance() {
        let symbols = MonarchAiPlanStepState.allCases.map { MonarchAiChainOfStepsTokens.symbol($0) }
        #expect(Set(symbols).count == MonarchAiPlanStepState.allCases.count)
        #expect(MonarchAiChainOfStepsTokens.iconRole(.active) == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiChainOfStepsTokens.iconRole(.done) == \MonarchColorTokens.successStrong)
        #expect(MonarchAiChainOfStepsTokens.iconRole(.failed) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchAiChainOfStepsTokens.labelRole(.todo) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiChainOfStepsTokens.labelRole(.active) == \MonarchColorTokens.textDefault)
        #expect(MonarchAiChainOfStepsTokens.stateRole(.failed) == \MonarchColorTokens.dangerStrong)
    }
}
