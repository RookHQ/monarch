import Foundation
import SwiftUI

/// Every string one condition row shows or speaks, already resolved. A caller with its own
/// wording (such as ``MonarchQueryBuilder``, which names rules after their group) builds one.
struct MonarchConditionRowText {
    /// The visible title, such as "Condition 1".
    let title: String
    /// The spoken name of the row container.
    let name: String
    /// The spoken name of the field menu.
    let field: String
    /// The spoken name of the operator menu.
    let operatorLabel: String
    /// The spoken name of the value input.
    let value: String
    /// The spoken name of the remove button.
    let remove: String
    /// The empty choice of a field or select value.
    let choose: String
    /// The button that sets a date value.
    let chooseDate: String
    /// A stored field the list no longer has, from its key.
    let removedField: (String) -> String
    /// The visible name of an operator.
    let operatorName: (MonarchConditionOperator) -> String
    /// What to do about a problem.
    let problem: (MonarchConditionProblem) -> String
}

/// One condition, stacked for a phone: title and remove, field menu, operator menu, a value input
/// that suits the field, and the problem when one is shown. Shared by ``MonarchConditionEditor``
/// and ``MonarchQueryBuilder``.
struct MonarchConditionRow: View {
    @Binding var rule: MonarchCondition
    let fields: [MonarchConditionField]
    let text: MonarchConditionRowText
    let problem: MonarchConditionProblem?
    /// The day a date value starts on when "Choose a date" is tapped.
    let defaultDate: () -> Date
    let onRemove: () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private var field: MonarchConditionField? { fields.first { $0.id == rule.field } }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchQueryBuilderTokens.ruleSpacing) {
            if text.title.isEmpty {
                inlineRow
            } else {
                HStack(alignment: .firstTextBaseline) {
                    Text(text.title)
                        .monarchTextStyle(.label)
                        .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityHidden(true)
                    Button(action: onRemove) {
                        Label(text.remove, systemImage: MonarchQueryBuilderTokens.removeSymbol)
                            .labelStyle(.iconOnly)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
                fieldPicker
                if field != nil { operatorPicker }
                valueInput
            }
            if let problem {
                HStack(alignment: .firstTextBaseline, spacing: MonarchQueryBuilderTokens.messageIconSpacing) {
                    MonarchIcon(systemName: MonarchQueryBuilderTokens.problemSymbol, size: .sm)
                    Text(text.problem(problem)).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityHidden(true)
            }
        }
        .padding(MonarchQueryBuilderTokens.padding)
        .background {
            MonarchShapes.control.fill(theme.colors[role: MonarchQueryBuilderTokens.ruleFill])
            MonarchShapes.control.strokeBorder(
                theme.colors[role: MonarchQueryBuilderTokens.border(hasProblem: problem != nil)],
                lineWidth: MonarchShapes.borderWidth
            )
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.name)
        .accessibilityHint(problem.map(text.problem) ?? "")
    }

    /// A rule with no visible title, as the web: field, operator and value side by side with the remove button at
    /// the end, falling back to a stack when the width is too small.
    private var inlineRow: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: MonarchQueryBuilderTokens.ruleSpacing) {
                fieldPicker
                if field != nil { operatorPicker }
                valueInput
                Spacer(minLength: 0)
                removeButton
            }
            HStack(alignment: .center, spacing: MonarchQueryBuilderTokens.ruleSpacing) {
                VStack(alignment: .leading, spacing: MonarchQueryBuilderTokens.ruleSpacing) {
                    fieldPicker
                    if field != nil { operatorPicker }
                    valueInput
                }
                Spacer(minLength: 0)
                removeButton
            }
        }
    }

    private var removeButton: some View {
        Button(action: onRemove) {
            Label(text.remove, systemImage: MonarchQueryBuilderTokens.removeSymbol)
                .labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
    }

    private var fieldPicker: some View {
        MonarchSelectField(
            text.field,
            selection: Binding(
                get: { rule.field },
                set: { id in
                    rule = MonarchQueryBuilderTokens.changingField(rule, to: fields.first { $0.id == id })
                }
            ),
            valueText: field?.label ?? (rule.field.isEmpty ? text.choose : text.removedField(rule.field)),
            style: .compact
        ) {
            if rule.field.isEmpty {
                Text(text.choose).tag("")
            } else if field == nil {
                Text(text.removedField(rule.field)).tag(rule.field)
            }
            ForEach(fields.filter { $0.group == nil }) { field in
                Text(field.label).tag(field.id)
            }
            ForEach(groupNames, id: \.self) { name in
                Section(name) {
                    ForEach(fields.filter { $0.group == name }) { field in
                        Text(field.label).tag(field.id)
                    }
                }
            }
        }
    }

    /// The headings of the field menu, in the order they first occur.
    private var groupNames: [String] {
        var seen: [String] = []
        for name in fields.compactMap(\.group) where !seen.contains(name) { seen.append(name) }
        return seen
    }

    private var choices: [MonarchComboboxOption] {
        switch field?.kind {
        case .select(let options), .tags(let options): options
        default: []
        }
    }

    private var operatorPicker: some View {
        MonarchSelectField(
            text.operatorLabel,
            selection: Binding(
                get: { rule.operator },
                set: { op in rule = MonarchQueryBuilderTokens.changingOperator(rule, to: op, field: field) }
            ),
            valueText: text.operatorName(rule.operator),
            style: .compact
        ) {
            ForEach(MonarchQueryBuilderTokens.operators(for: rule, field: field), id: \.self) { op in
                Text(text.operatorName(op)).tag(op)
            }
        }
    }

    @ViewBuilder private var valueInput: some View {
        let label = text.value
        switch rule.value {
        case .none, .boolean:
            EmptyView()
        case .text(let current):
            MonarchQueryBuilderValueField(
                label: label,
                text: Binding(get: { current }, set: { rule.value = .text($0) }),
                prompt: field?.prompt,
                isNumber: false
            )
        case .number(let current):
            MonarchQueryBuilderNumberField(
                label: label,
                value: Binding(get: { current }, set: { rule.value = .number($0) }),
                prompt: field?.prompt
            )
        case .date(let current):
            if let current {
                DatePicker(
                    label,
                    selection: Binding(get: { current }, set: { rule.value = .date($0) }),
                    displayedComponents: .date
                )
                .labelsHidden()
                .accessibilityLabel(label)
                .monarchMinimumHitTarget()
            } else {
                Button(text.chooseDate, systemImage: "calendar") { rule.value = .date(defaultDate()) }
                    .buttonStyle(.monarch(.secondary, size: .sm))
                    .accessibilityHint(label)
            }
        case .options(let current):
            MonarchMultiSelect(
                label,
                selection: Binding(get: { current }, set: { rule.value = .options($0) }),
                options: choices,
                prompt: text.choose
            )
        case .option(let current):
            MonarchSelectField(
                label,
                selection: Binding(get: { current }, set: { rule.value = .option($0) }),
                valueText: current.flatMap { id in
                    if case .select(let options) = field?.kind { options.first { $0.id == id }?.label } else { nil }
                },
                prompt: text.choose,
                style: .compact
            ) {
                Text(text.choose).tag(String?.none)
                if case .select(let options) = field?.kind {
                    ForEach(options) { option in
                        Text(option.label).tag(String?.some(option.id))
                            .disabled(option.isDisabled)
                    }
                }
            }
        }
    }
}
