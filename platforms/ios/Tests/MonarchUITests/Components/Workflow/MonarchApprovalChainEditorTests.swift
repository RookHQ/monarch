import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchApprovalChainEditorFixtures: ComponentFixtures {
    static let id = "approval-chain-editor"

    static let types = [
        MonarchApprovalApproverType(id: "manager", label: "Reporting manager"),
        MonarchApprovalApproverType(
            id: "role",
            label: "Anyone with a role",
            targets: [
                MonarchComboboxOption(id: "finance", label: "Finance team"),
                MonarchComboboxOption(id: "legal", label: "Legal"),
            ],
            targetLabel: "Role"
        ),
        MonarchApprovalApproverType(
            id: "person",
            label: "A specific person",
            targets: [MonarchComboboxOption(id: "ada", label: "Ada Moss")]
        ),
    ]

    static let steps = [
        MonarchApprovalChainStep(id: "s1", approver: "manager"),
        MonarchApprovalChainStep(id: "s2", approver: "role", target: "finance", label: "Finance check"),
        MonarchApprovalChainStep(id: "s3", approver: "person"),
    ]

    static let fixtures: [Fixture] = [
        Fixture("three steps") {
            MonarchApprovalChainEditor(
                steps: .constant(steps),
                approverTypes: types,
                description: "Each step must approve before the next is asked.",
                makeID: { "new" }
            )
        },
        Fixture("errors shown") {
            MonarchApprovalChainEditor(
                steps: .constant(steps),
                approverTypes: types,
                showsErrors: true,
                makeID: { "new" }
            )
        },
        Fixture("one step at the minimum, no labels") {
            MonarchApprovalChainEditor(
                steps: .constant([steps[0]]),
                approverTypes: types,
                showsLabels: false,
                makeID: { "new" }
            )
        },
        Fixture("full chain") {
            MonarchApprovalChainEditor(steps: .constant(steps), approverTypes: types, maxSteps: 3, makeID: { "new" })
        },
        Fixture("unknown approver, disabled") {
            MonarchApprovalChainEditor(
                steps: .constant([MonarchApprovalChainStep(id: "x", approver: "retired-type")]),
                approverTypes: types,
                makeID: { "new" }
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchApprovalChainEditor")
struct MonarchApprovalChainEditorTests {
    private typealias Tokens = MonarchApprovalChainEditorTokens
    private let types = MonarchApprovalChainEditorFixtures.types
    private let steps = MonarchApprovalChainEditorFixtures.steps

    @Test("Steps whose type needs a target and has none are invalid")
    func validate() {
        #expect(MonarchApprovalChainEditor.validate(steps, approverTypes: types) == ["s3"])
        let fixed = steps.map {
            $0.id == "s3" ? MonarchApprovalChainStep(id: "s3", approver: "person", target: "ada") : $0
        }
        #expect(MonarchApprovalChainEditor.validate(fixed, approverTypes: types).isEmpty)
        let stale = [MonarchApprovalChainStep(id: "a", approver: "role", target: "gone")]
        #expect(MonarchApprovalChainEditor.validate(stale, approverTypes: types) == ["a"])
        let unknown = [MonarchApprovalChainStep(id: "b", approver: "unknown")]
        #expect(MonarchApprovalChainEditor.validate(unknown, approverTypes: types).isEmpty)
    }

    @Test("The chain reads as words")
    func summarize() {
        #expect(
            MonarchApprovalChainEditor.summarize(steps, approverTypes: types)
                == "Reporting manager → Finance check (Anyone with a role: Finance team) → A specific person"
        )
        #expect(
            MonarchApprovalChainEditor.summarize([steps[0], steps[0]], approverTypes: types, separator: ", ")
                == "Reporting manager, Reporting manager"
        )
        #expect(Tokens.stepSummary(MonarchApprovalChainStep(id: "u", approver: "unknown"), types: types) == "unknown")
    }

    @Test("Changing the approver drops a target the new type does not offer")
    func changingApprover() {
        let step = steps[1]
        #expect(Tokens.changingApprover(step, to: "manager", types: types).target == nil)
        #expect(Tokens.changingApprover(step, to: "manager", types: types).label == "Finance check")
        #expect(Tokens.changingApprover(step, to: "role", types: types).target == "finance")
        #expect(Tokens.changingApprover(step, to: "person", types: types).target == nil)
    }

    @Test("Labels are cut to 60 characters and empty becomes nil")
    func cleanedLabel() {
        #expect(Tokens.cleanedLabel("") == nil)
        #expect(Tokens.cleanedLabel("Finance") == "Finance")
        #expect(Tokens.cleanedLabel(String(repeating: "a", count: 80))?.count == 60)
    }

    @Test("Moving swaps neighbours and stops at the ends")
    func moving() {
        #expect(Tokens.moving(steps, id: "s2", by: -1).map(\.id) == ["s2", "s1", "s3"])
        #expect(Tokens.moving(steps, id: "s2", by: 1).map(\.id) == ["s1", "s3", "s2"])
        #expect(Tokens.moving(steps, id: "s1", by: -1) == steps)
        #expect(Tokens.moving(steps, id: "s3", by: 1) == steps)
        #expect(Tokens.moving(steps, id: "missing", by: 1) == steps)
    }

    @Test("Remove and add follow the fewest and most steps; new steps use the first type")
    func limits() {
        #expect(!Tokens.canRemove(count: 1, minSteps: 1))
        #expect(Tokens.canRemove(count: 2, minSteps: 1))
        #expect(Tokens.canAdd(count: 9, maxSteps: 10))
        #expect(!Tokens.canAdd(count: 10, maxSteps: 10))
        #expect(Tokens.newStep(types: types, id: "n") == MonarchApprovalChainStep(id: "n", approver: "manager"))
        #expect(Tokens.newStep(types: [], id: "n").approver == "")
    }

    @Test("A step is read as its title and summary; labels override")
    func spoken() {
        let none = MonarchApprovalChainEditorLabels()
        #expect(
            Tokens.accessibilityLabel(position: 1, step: steps[0], types: types, labels: none)
                == "Step 1, Reporting manager"
        )
        let custom = MonarchApprovalChainEditorLabels(step: { "Stufe \($0)" })
        #expect(Tokens.stepTitle(2, labels: custom) == "Stufe 2")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchApprovalChainEditorStrings.legend, MonarchApprovalChainEditorStrings.approver,
            MonarchApprovalChainEditorStrings.target, MonarchApprovalChainEditorStrings.targetPlaceholder,
            MonarchApprovalChainEditorStrings.label, MonarchApprovalChainEditorStrings.labelPlaceholder,
            MonarchApprovalChainEditorStrings.addStep, MonarchApprovalChainEditorStrings.targetRequired,
            MonarchApprovalChainEditorStrings.moveUp, MonarchApprovalChainEditorStrings.moveDown,
            MonarchApprovalChainEditorStrings.step(1), MonarchApprovalChainEditorStrings.removeStep(1),
            MonarchApprovalChainEditorStrings.maxReached(10), MonarchApprovalChainEditorStrings.reorder(1),
            MonarchApprovalChainEditorStrings.moved(2),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchApprovalChainEditorStrings.removeStep(3) == "Remove step 3")
    }
}
