import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let teams = [
    MonarchComboboxOption(id: "billing", label: "Billing"),
    MonarchComboboxOption(id: "support", label: "Support"),
    MonarchComboboxOption(id: "sales", label: "Sales"),
]

private let actionTypes = [
    MonarchActionType(
        id: "team",
        label: "Set team",
        description: "Moves the ticket to a team's queue.",
        group: "Assignment",
        parameters: [
            MonarchActionParameter(key: "team", label: "Team", kind: .select(teams), isRequired: true),
            MonarchActionParameter(key: "notify", label: "Notify the team", kind: .boolean, description: "By email."),
            MonarchActionParameter(
                key: "message",
                label: "Message",
                kind: .multilineText,
                isRequired: true,
                visibleWhen: { $0["notify"] == .boolean(true) }
            ),
        ]
    ),
    MonarchActionType(
        id: "tags",
        label: "Add tags",
        parameters: [
            MonarchActionParameter(
                key: "tags",
                label: "Tags",
                kind: .tags(suggestions: ["vip", "refund"]),
                isRequired: true
            )
        ],
        defaults: ["tags": .list(["vip"])]
    ),
    MonarchActionType(
        id: "wait",
        label: "Wait",
        group: "Timing",
        parameters: [
            MonarchActionParameter(
                key: "hours",
                label: "Hours",
                kind: .number(minimum: 1, maximum: 72),
                isRequired: true
            ),
            MonarchActionParameter(key: "channels", label: "Channels", kind: .multiSelect(teams)),
            MonarchActionParameter(key: "ref", label: "Reference", kind: .text, prompt: "Optional"),
        ],
        validate: { values in
            if case .number(let hours?) = values["hours"], hours > 48 {
                return "Waits longer than 48 hours pause SLAs."
            }
            return nil
        }
    ),
    MonarchActionType(
        id: "sms",
        label: "Send SMS",
        group: "Assignment",
        isDisabled: true,
        disabledReason: "Plan limit"
    ),
]

private let actionsFixture = [
    MonarchRuleAction(id: "a1", type: "team", parameters: ["team": .option("support"), "notify": .boolean(true)]),
    MonarchRuleAction(id: "a2", type: "tags", parameters: ["tags": .list(["vip"])]),
    MonarchRuleAction(
        id: "a3",
        type: "wait",
        parameters: ["hours": .number(60), "channels": .list(["billing", "sales"])]
    ),
]

enum MonarchActionEditorFixtures: ComponentFixtures {
    static let id = "action-editor"
    static let fixtures = [
        Fixture("every input kind") {
            MonarchActionEditor(actions: .constant(actionsFixture), types: actionTypes)
        },
        Fixture("errors shown, disabled and unknown types") {
            MonarchActionEditor(
                actions: .constant(
                    actionsFixture + [
                        MonarchRuleAction(id: "a4", type: "sms"), MonarchRuleAction(id: "a5", type: "escalate"),
                    ]
                ),
                types: actionTypes,
                showsErrors: true
            )
        },
        Fixture("empty, full, unordered") {
            VStack(alignment: .leading) {
                MonarchActionEditor(actions: .constant([]), types: actionTypes)
                MonarchActionEditor(
                    actions: .constant(Array(actionsFixture.prefix(1))),
                    types: actionTypes,
                    maxActions: 1
                )
                MonarchActionEditor(actions: .constant(actionsFixture), types: actionTypes, isOrdered: false)
            }
        },
        Fixture("disabled") {
            MonarchActionEditor(actions: .constant(actionsFixture), types: actionTypes).disabled(true)
        },
    ]
}

@Suite("MonarchActionEditor")
struct MonarchActionEditorTests {
    private let text = MonarchActionEditorText(labels: MonarchActionEditorLabels())

    @Test("A new action starts with its type's defaults")
    func newAction() {
        let action = MonarchRuleAction(id: "n", type: actionTypes[1])
        #expect(action.type == "tags")
        #expect(action.parameters == ["tags": .list(["vip"])])
        #expect(MonarchRuleAction(id: "n", type: nil).type.isEmpty)
    }

    @Test("New actions start as the preferred type, else the first that is enabled")
    func startType() {
        #expect(MonarchActionEditorTokens.startType(actionTypes, preferred: "wait")?.id == "wait")
        #expect(MonarchActionEditorTokens.startType(actionTypes, preferred: "missing")?.id == "team")
        #expect(MonarchActionEditorTokens.startType([actionTypes[3], actionTypes[1]], preferred: nil)?.id == "tags")
        #expect(MonarchActionEditorTokens.startType([actionTypes[3]], preferred: nil) == nil)
    }

    @Test("Problems are typed: missing, out of range and the type's own check")
    func problems() {
        let first = MonarchActionEditor.problems(of: actionsFixture[0], types: actionTypes)
        #expect(first == [.missing(parameter: "message")])
        let quiet = MonarchRuleAction(id: "q", type: "team", parameters: ["team": .option("support")])
        #expect(MonarchActionEditor.problems(of: quiet, types: actionTypes).isEmpty)
        #expect(
            MonarchActionEditor.problems(of: actionsFixture[2], types: actionTypes) == [
                .invalid(message: "Waits longer than 48 hours pause SLAs.")
            ]
        )
        let empty = MonarchRuleAction(id: "e", type: "tags", parameters: ["tags": .list([])])
        #expect(MonarchActionEditor.problems(of: empty, types: actionTypes) == [.missing(parameter: "tags")])
        #expect(MonarchActionEditor.problems(of: MonarchRuleAction(id: "u", type: "gone"), types: actionTypes).isEmpty)
        let all = MonarchActionEditor.problems(of: actionsFixture, types: actionTypes)
        #expect(Set(all.keys) == ["a1", "a3"])
    }

    @Test("A number outside its range is a problem of its own, and the empty number is missing")
    func outOfRange() {
        func problems(_ hours: Double?) -> [MonarchActionProblem] {
            MonarchActionEditor.problems(
                of: MonarchRuleAction(id: "w", type: "wait", parameters: ["hours": .number(hours)]),
                types: actionTypes
            )
        }
        #expect(problems(0) == [.outOfRange(parameter: "hours")])
        // 72 is the maximum, so it is in range; the type's own check still objects to more than 48.
        let tooLong = MonarchActionProblem.invalid(message: "Waits longer than 48 hours pause SLAs.")
        #expect(problems(72) == [tooLong])
        #expect(problems(73) == [.outOfRange(parameter: "hours"), tooLong])
        #expect(problems(nil) == [.missing(parameter: "hours")])
        #expect(problems(24).isEmpty)
        // The type's own check follows the range check.
        #expect(problems(60) == [tooLong])
    }

    @Test("A problem becomes the text of its parameter, in the built-in English or the app's words")
    func problemMessages() {
        let wait = actionTypes[2]
        #expect(MonarchActionEditor.message(for: .missing(parameter: "hours"), in: wait) == "Fill in “Hours”.")
        let range = MonarchActionEditor.message(for: .outOfRange(parameter: "hours"), in: wait)
        #expect(range == "“Hours” is out of range.")
        #expect(MonarchActionEditor.message(for: .invalid(message: "Too long."), in: wait) == "Too long.")
        let labels = MonarchActionEditorLabels(required: { "\($0) is needed" }, outOfRange: { "\($0) is too big" })
        let needed = MonarchActionEditor.message(for: .missing(parameter: "hours"), in: wait, labels: labels)
        let tooBig = MonarchActionEditor.message(for: .outOfRange(parameter: "hours"), in: wait, labels: labels)
        #expect(needed == "Hours is needed")
        #expect(tooBig == "Hours is too big")
        #expect(MonarchActionEditor.message(for: .missing(parameter: "gone"), in: wait) == "Fill in “gone”.")
    }

    @Test("Hidden inputs are skipped")
    func visible() {
        let hidden = MonarchActionEditorTokens.visibleParameters(actionTypes[0], values: [:]).map(\.key)
        #expect(hidden == ["team", "notify"])
        let shown = MonarchActionEditorTokens.visibleParameters(actionTypes[0], values: ["notify": .boolean(true)])
        #expect(shown.map(\.key) == ["team", "notify", "message"])
    }

    @Test("Empty values: whitespace text, no number, no option, empty list; booleans never")
    func emptiness() {
        #expect(MonarchActionValue.text("  ").isEmpty)
        #expect(MonarchActionValue.number(nil).isEmpty)
        #expect(!MonarchActionValue.number(0).isEmpty)
        #expect(MonarchActionValue.option(nil).isEmpty)
        #expect(MonarchActionValue.list([]).isEmpty)
        #expect(!MonarchActionValue.boolean(false).isEmpty)
    }

    @Test("Moving swaps with the neighbour and stops at the ends")
    func moving() {
        let up = MonarchActionEditorTokens.moving(actionsFixture, id: "a2", by: -1)
        #expect(up.map(\.id) == ["a2", "a1", "a3"])
        #expect(MonarchActionEditorTokens.moving(actionsFixture, id: "a1", by: -1) == actionsFixture)
        #expect(MonarchActionEditorTokens.moving(actionsFixture, id: "a3", by: 1) == actionsFixture)
        #expect(MonarchActionEditorTokens.moving(actionsFixture, id: "zz", by: 1) == actionsFixture)
    }

    @Test("The type menu lists ungrouped types first, then groups in order")
    func sections() {
        let sections = MonarchActionEditorTokens.sections(actionTypes)
        #expect(sections.map(\.title) == [nil, "Assignment", "Timing"])
        #expect(sections[1].types.map(\.id) == ["team", "sms"])
        #expect(MonarchActionEditorTokens.menuLabel(actionTypes[3]) == "Send SMS (Plan limit)")
        #expect(MonarchActionEditorTokens.menuLabel(actionTypes[0]) == "Set team")
    }

    @Test("Multi-select text and order follow the options")
    func multiSelect() {
        #expect(MonarchActionEditorTokens.multiSelectSummary([], options: teams, prompt: nil, text: text) == "Choose…")
        #expect(
            MonarchActionEditorTokens.multiSelectSummary(["sales"], options: teams, prompt: nil, text: text) == "Sales"
        )
        #expect(
            MonarchActionEditorTokens.multiSelectSummary(["sales", "billing"], options: teams, prompt: nil, text: text)
                == "2 chosen"
        )
        #expect(
            MonarchActionEditorTokens.toggling("billing", isOn: true, in: ["sales"], options: teams) == [
                "billing", "sales",
            ]
        )
        #expect(
            MonarchActionEditorTokens.toggling("sales", isOn: false, in: ["sales", "old"], options: teams) == ["old"]
        )
    }

    @Test("Spoken names and warnings")
    func accessibility() {
        #expect(
            MonarchActionEditorTokens.accessibilityLabel(
                position: 1,
                action: actionsFixture[0],
                type: actionTypes[0],
                text: text
            ) == "Action 1, Set team"
        )
        #expect(
            MonarchActionEditorTokens.accessibilityLabel(
                position: 2,
                action: MonarchRuleAction(id: "x", type: "escalate"),
                type: nil,
                text: text
            ) == "Action 2, escalate (not available)"
        )
        #expect(
            MonarchActionEditorTokens.unavailableMessage(actionTypes[3], text: text)
                == "This action cannot run right now. Plan limit"
        )
        #expect(text.remove(3) == "Remove action 3")
        #expect(text.reorder(1) == "Reorder action 1")
        #expect(text.moved(2) == "Moved to position 2.")
    }

    @Test("Built-in strings come from the table and can be replaced")
    func strings() {
        let all = [
            text.label, text.type, text.add, text.empty, text.choose, text.unavailable, text.moveUp, text.moveDown,
            text.maxReached(20), text.chosen(2), text.action(1),
        ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(text.maxReached(20) == "You can add up to 20 actions.")
        let custom = MonarchActionEditorText(
            labels: MonarchActionEditorLabels(action: { "Step \($0)" }, add: "New", required: { "\($0)!" })
        )
        #expect(custom.action(2) == "Step 2")
        #expect(custom.add == "New")
        #expect(custom.required("Team") == "Team!")
    }
}
