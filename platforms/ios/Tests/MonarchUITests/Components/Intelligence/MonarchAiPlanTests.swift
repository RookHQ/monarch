import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAiPlanFixtures: ComponentFixtures {
    static let id = "ai-plan"
    static let steps = [
        MonarchAiPlanStep(id: "find", label: "Find the three open invoices"),
        MonarchAiPlanStep(id: "draft", label: "Draft a reminder for each customer"),
        MonarchAiPlanStep(id: "send", label: "Send the reminders"),
    ]
    static let running = [
        MonarchAiPlanStep(id: "find", label: "Find the three open invoices", state: .done),
        MonarchAiPlanStep(id: "draft", label: "Draft a reminder for each customer", state: .active),
        MonarchAiPlanStep(id: "send", label: "Send the reminders"),
    ]

    static let fixtures = [
        Fixture("pending, editable") {
            MonarchAiPlan(MonarchAiPlanPart(steps: steps))
        },
        Fixture("pending, read-only") {
            MonarchAiPlan(MonarchAiPlanPart(steps: steps), isEditable: false)
        },
        Fixture("approved, running") {
            MonarchAiPlan(MonarchAiPlanPart(steps: running), decision: .approved)
        },
        Fixture("rejected") {
            MonarchAiPlan(MonarchAiPlanPart(steps: steps), decision: .rejected)
        },
        Fixture("empty step, approve disabled") {
            MonarchAiPlan(MonarchAiPlanPart(steps: [MonarchAiPlanStep(id: "x", label: "  ")]))
        },
        Fixture("long step") {
            MonarchAiPlan(
                MonarchAiPlanPart(steps: [
                    MonarchAiPlanStep(
                        id: "long",
                        label:
                            "Compare every clause of the supplier agreement with the standard terms and list each difference"
                    )
                ]),
                labels: MonarchAiPlanLabels(title: "Contract review")
            )
        },
    ]
}

@Suite("MonarchAiPlan")
struct MonarchAiPlanTests {
    private let steps = MonarchAiPlanFixtures.steps
    private let labels = MonarchAiPlanLabels()

    @Test("A decided or non-editable plan is read-only")
    func readOnly() {
        #expect(!MonarchAiPlanTokens.isReadOnly(decision: .pending, isEditable: true))
        #expect(MonarchAiPlanTokens.isReadOnly(decision: .pending, isEditable: false))
        #expect(MonarchAiPlanTokens.isReadOnly(decision: .approved, isEditable: true))
        #expect(MonarchAiPlanTokens.isReadOnly(decision: .rejected, isEditable: true))
    }

    @Test("Approve and Reject show while pending, unless the app owns the decision and listens to nothing")
    func decisionButtons() {
        #expect(MonarchAiPlanTokens.showsDecision(decision: .pending, isControlled: false, hasHandlers: false))
        #expect(MonarchAiPlanTokens.showsDecision(decision: .pending, isControlled: true, hasHandlers: true))
        #expect(!MonarchAiPlanTokens.showsDecision(decision: .pending, isControlled: true, hasHandlers: false))
        #expect(!MonarchAiPlanTokens.showsDecision(decision: .approved, isControlled: false, hasHandlers: true))
    }

    @Test("Approved steps are trimmed and empty steps dropped")
    func cleaning() {
        let messy = [
            MonarchAiPlanStep(id: "a", label: "  Find invoices "),
            MonarchAiPlanStep(id: "b", label: "   "),
            MonarchAiPlanStep(id: "c", label: "Send", state: .done),
        ]
        let cleaned = MonarchAiPlanTokens.cleaned(messy)
        #expect(cleaned.map(\.id) == ["a", "c"])
        #expect(cleaned.map(\.label) == ["Find invoices", "Send"])
        #expect(cleaned[1].state == .done)
        #expect(MonarchAiPlanTokens.shownSteps(messy, decision: .pending).count == 3)
        #expect(MonarchAiPlanTokens.shownSteps(messy, decision: .approved).count == 2)
    }

    @Test("Moving swaps one place and stops at the ends")
    func moving() {
        let down = MonarchAiPlanTokens.moving(steps, id: "find", direction: .down)
        #expect(down?.steps.map(\.id) == ["draft", "find", "send"])
        #expect(down?.index == 1)
        #expect(
            MonarchAiPlanTokens.moving(steps, id: "send", direction: .backward)?.steps.map(\.id) == [
                "find", "send", "draft",
            ]
        )
        #expect(MonarchAiPlanTokens.moving(steps, id: "find", direction: .up) == nil)
        #expect(MonarchAiPlanTokens.moving(steps, id: "send", direction: .forward) == nil)
        #expect(MonarchAiPlanTokens.moving(steps, id: "missing", direction: .down) == nil)
    }

    @Test("Dropping takes the target's place")
    func dropping() {
        let forward = MonarchAiPlanTokens.dropping(steps, id: "find", onto: "send")
        #expect(forward?.steps.map(\.id) == ["draft", "send", "find"])
        #expect(forward?.index == 2)
        let back = MonarchAiPlanTokens.dropping(steps, id: "send", onto: "find")
        #expect(back?.steps.map(\.id) == ["send", "find", "draft"])
        #expect(MonarchAiPlanTokens.dropping(steps, id: "find", onto: "find") == nil)
        #expect(MonarchAiPlanTokens.dropping(steps, id: "x", onto: "find") == nil)
    }

    @Test("New steps are empty, inserted after an index, with unused ids")
    func inserting() {
        let added = MonarchAiPlanTokens.inserting(steps, newID: "new-1", after: 2)
        #expect(added.map(\.id) == ["find", "draft", "send", "new-1"])
        #expect(added[3].label.isEmpty && added[3].state == .todo)
        #expect(MonarchAiPlanTokens.inserting([], newID: "n", after: -1).map(\.id) == ["n"])
        let taken = steps + [MonarchAiPlanStep(id: "new-1", label: "x")]
        #expect(MonarchAiPlanTokens.newID(counter: 1, existing: taken) == "new-2")
    }

    @Test("Spoken names and announcements")
    func accessibility() {
        #expect(MonarchAiPlanTokens.name(of: steps[0], at: 0, labels: labels) == "Find the three open invoices")
        #expect(MonarchAiPlanTokens.name(of: MonarchAiPlanStep(id: "e", label: " "), at: 1, labels: labels) == "step 2")
        #expect(MonarchAiPlanStrings.stepLabel(2) == "Step 2")
        #expect(MonarchAiPlanStrings.reorder("Send") == "Reorder Send")
        #expect(MonarchAiPlanStrings.moved("Send", position: 1, total: 3) == "Send, position 1 of 3")
        #expect(MonarchAiPlanStrings.remove("Send") == "Remove Send")
        #expect(MonarchAiPlanStrings.removed("Send") == "Removed Send")
        #expect(MonarchAiPlanTokens.announcement(for: .pending, labels: labels) == nil)
        #expect(
            MonarchAiPlanTokens.announcement(for: .approved, labels: labels)
                == "Plan approved. The assistant is working through it."
        )
        let custom = MonarchAiPlanLabels(rejectedNotice: "Refusé")
        #expect(MonarchAiPlanTokens.announcement(for: .rejected, labels: custom) == "Refusé")
    }

    @Test("The description follows the decision and whether the plan can be edited")
    func description() {
        let base = "The assistant will take these steps after you approve. Nothing runs until then."
        #expect(MonarchAiPlanTokens.description(decision: .pending, isReadOnly: true, labels: labels) == base)
        #expect(
            MonarchAiPlanTokens.description(decision: .pending, isReadOnly: false, labels: labels)
                == "\(base) You can edit, reorder, add or remove steps first."
        )
        #expect(
            MonarchAiPlanTokens.description(decision: .rejected, isReadOnly: true, labels: labels)
                == "Plan rejected. The assistant will not take these steps."
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAiPlanStrings.title == "Proposed plan")
        #expect(MonarchAiPlanStrings.addStep == "Add step")
        #expect(MonarchAiPlanStrings.approve == "Approve plan")
        #expect(MonarchAiPlanStrings.reject == "Reject")
        #expect(MonarchAiPlanStrings.stepPlaceholder == "Describe this step")
        #expect(MonarchAiPlanStrings.emptyPlan == "Add at least one step to approve the plan.")
    }

    @Test("Colours are text and AI roles")
    func appearance() {
        #expect(MonarchAiPlanTokens.title == \MonarchColorTokens.textDefault)
        #expect(MonarchAiPlanTokens.muted == \MonarchColorTokens.textMuted)
        #expect(MonarchAiPlanTokens.dropTarget == \MonarchColorTokens.aiSubtle)
    }
}
