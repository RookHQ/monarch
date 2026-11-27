import MonarchUI
import SwiftUI

struct MonarchActionEditorSection: CataloguePage {
    static let title = "Action editor"

    @State private var actions = [
        MonarchRuleAction(id: "a1", type: "team", parameters: ["team": .option("support")]),
        MonarchRuleAction(id: "a2", type: "priority", parameters: ["priority": .option("high")]),
    ]
    @State private var showsErrors = false

    private let teams = [
        MonarchComboboxOption(id: "billing", label: "Billing"),
        MonarchComboboxOption(id: "support", label: "Support"),
    ]

    private var types: [MonarchActionType] {
        [
            MonarchActionType(
                id: "team",
                label: "Set team",
                description: "Moves the ticket to the team's queue.",
                group: "Assignment",
                parameters: [
                    MonarchActionParameter(key: "team", label: "Team", kind: .select(teams), isRequired: true),
                    MonarchActionParameter(key: "notify", label: "Notify the team", kind: .boolean),
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
                id: "priority",
                label: "Set priority",
                group: "Assignment",
                parameters: [
                    MonarchActionParameter(
                        key: "priority",
                        label: "Priority",
                        kind: .select([
                            MonarchComboboxOption(id: "high", label: "High"),
                            MonarchComboboxOption(id: "normal", label: "Normal"),
                        ]),
                        isRequired: true
                    )
                ]
            ),
            MonarchActionType(
                id: "tags",
                label: "Add tags",
                parameters: [
                    MonarchActionParameter(key: "tags", label: "Tags", kind: .tags(suggestions: ["vip", "refund"]))
                ]
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
                    )
                ]
            ),
            MonarchActionType(
                id: "sms",
                label: "Send SMS",
                group: "Timing",
                isDisabled: true,
                disabledReason: "Plan limit"
            ),
        ]
    }

    var body: some View {
        MonarchActionEditor(actions: $actions, types: types, showsErrors: showsErrors)
        MonarchButton("Save rule", variant: .primary) {
            showsErrors = !MonarchActionEditor.problems(of: actions, types: types).isEmpty
        }
    }
}
