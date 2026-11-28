import MonarchTokens
import MonarchUI
import SwiftUI

/// Recipe: a "when, if, then" rule form in a sheet, composed of the condition and action editors.
struct RuleBuilderSection: CataloguePage {
    static let title = "Rule builder (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var name = ""
    @State private var triggers: Set<String> = []
    @State private var match = MonarchConditionMatch.all
    @State private var conditions: [MonarchCondition] = []
    @State private var actions: [MonarchRuleAction] = []
    @State private var isOn = true
    @State private var stopsProcessing = false
    @State private var showsErrors = false
    @State private var isOpen = false
    @State private var savedRules = 0
    @AccessibilityFocusState private var focusesSummary: Bool

    private let events = [("created", "Ticket created"), ("updated", "Ticket updated")]
    private let fields = [
        MonarchConditionField(
            id: "priority",
            label: "Priority",
            kind: .select([
                MonarchComboboxOption(id: "high", label: "High"),
                MonarchComboboxOption(id: "low", label: "Low"),
            ])
        ),
        MonarchConditionField(id: "subject", label: "Subject", kind: .text),
    ]
    private let types = [
        MonarchActionType(
            id: "team",
            label: "Set team",
            parameters: [
                MonarchActionParameter(
                    key: "team",
                    label: "Team",
                    kind: .select([
                        MonarchComboboxOption(id: "billing", label: "Billing"),
                        MonarchComboboxOption(id: "support", label: "Support"),
                    ]),
                    isRequired: true
                )
            ]
        )
    ]

    private var problems: [String] {
        var out: [String] = []
        if name.trimmingCharacters(in: .whitespaces).isEmpty { out.append("Give the rule a name.") }
        if triggers.isEmpty { out.append("Pick at least one trigger.") }
        if actions.isEmpty { out.append("Add at least one action.") }
        if conditions.contains(where: { $0.problem(fields: fields) != nil }) {
            out.append("Finish or remove the incomplete conditions.")
        }
        if !MonarchActionEditor.problems(of: actions, types: types).isEmpty {
            out.append("Finish or remove the incomplete actions.")
        }
        return out
    }

    private func trigger(_ id: String) -> Binding<Bool> {
        Binding(
            get: { triggers.contains(id) },
            set: { isOn in if isOn { triggers.insert(id) } else { triggers.remove(id) } }
        )
    }

    private func save() {
        showsErrors = true
        if problems.isEmpty {
            savedRules += 1
            isOpen = false
        } else {
            // The summary appears in this update; move VoiceOver to it in the next one.
            Task { @MainActor in focusesSummary = true }
        }
    }

    var body: some View {
        MonarchButton("New rule", systemImage: "plus", variant: .primary) {
            showsErrors = false
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: MonarchTokens.Space.space4) {
                        MonarchTextField(
                            "Name",
                            text: $name,
                            error: showsErrors && name.trimmingCharacters(in: .whitespaces).isEmpty
                                ? "Give the rule a name." : nil
                        )
                        MonarchSectionCard("When", description: "The rule is checked whenever any of these happen.") {
                            ForEach(events, id: \.0) { event in
                                Toggle(event.1, isOn: trigger(event.0))
                                    .toggleStyle(.monarchCheckbox)
                            }
                            if showsErrors && triggers.isEmpty {
                                MonarchInlineNotice("Pick at least one trigger.", tone: .danger, variant: .plain)
                            }
                        }
                        MonarchSectionCard("If", description: "Leave empty to apply every time the rule is checked.") {
                            MonarchConditionEditor(
                                conditions: $conditions,
                                fields: fields,
                                match: $match,
                                showsErrors: showsErrors
                            )
                        }
                        MonarchSectionCard("Then", description: "Actions run in this order.") {
                            MonarchActionEditor(actions: $actions, types: types, showsErrors: showsErrors)
                            if showsErrors && actions.isEmpty {
                                MonarchInlineNotice("Add at least one action.", tone: .danger, variant: .plain)
                            }
                        }
                        MonarchSectionCard("Options") {
                            Toggle("Rule is on", isOn: $isOn)
                            Toggle("Stop processing later rules when this one matches", isOn: $stopsProcessing)
                        }
                        if showsErrors && !problems.isEmpty {
                            MonarchBanner("Not saved yet", message: problems.joined(separator: "\n"), tone: .danger)
                                .accessibilityElement(children: .combine)
                                .accessibilityFocused($focusesSummary)
                        }
                    }
                    .padding()
                }
                .navigationTitle("New rule")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isOpen = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save rule") { save() }
                    }
                }
            }
        }
        Text("Saved rules: \(savedRules)")
            .monarchTextStyle(.body)
        Text("Save with empty parts to see the problems.")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
