import Foundation
import SwiftUI

/// Edits a flat list of conditions (field, operator and a value that suits the field) with an
/// all or any switch. Fields are data, so the same editor serves ticket rules, form logic and
/// eligibility checks.
///
/// ```swift
/// @State private var conditions = [
///     MonarchCondition(id: "c1", field: "priority", operator: .equals, value: .option("high")),
/// ]
/// @State private var match = MonarchConditionMatch.all
///
/// MonarchConditionEditor(conditions: $conditions, fields: [
///     MonarchConditionField(id: "priority", label: "Priority", kind: .select([
///         MonarchComboboxOption(id: "high", label: "High"),
///         MonarchComboboxOption(id: "low", label: "Low"),
///     ])),
///     MonarchConditionField(id: "hours", label: "Hours since created", kind: .number, prompt: "Hours"),
/// ], match: $match)
/// ```
///
/// A condition is a ``MonarchCondition`` and a field a ``MonarchConditionField``, the same values
/// ``MonarchQueryBuilder`` edits, so a flat list can grow into a nested query without a
/// conversion. Each row stacks a field menu and an operator menu (`Picker` with the menu style)
/// over a value input: a text field, a number field, a `DatePicker`, a menu of choices, or
/// nothing for operators that take no value. The switch is a segmented `Picker` read as
/// "Match all of these conditions". Check the list with
/// ``MonarchCondition/problem(fields:)`` before saving and pass `showsErrors` to show what is
/// missing; test a record with
/// `MonarchQueryGroup(id:combinator:rules:).matches(_:calendar:)`. Disable the editor with
/// `.disabled(true)`.
///
/// Accessibility: the list is a container named "Conditions" and each row a container named
/// "Condition 1". Its menus, value and remove button carry the position ("Condition 1 field",
/// "Remove condition 1"). A shown problem is text with an icon and is read as the row's hint.
/// A removal is announced. Touch targets are at least 44 points.
public struct MonarchConditionEditor: View {
    @Binding private var conditions: [MonarchCondition]
    private let fields: [MonarchConditionField]
    private let match: Binding<MonarchConditionMatch>?
    private let maxConditions: Int
    private let showsErrors: Bool
    private let defaultField: String?
    private let today: () -> Date
    private let text: MonarchConditionEditorText
    private let makeID: @MainActor () -> String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A condition editor.
    ///
    /// - Parameters:
    ///   - conditions: The conditions, in order.
    ///   - fields: The fields a condition can test.
    ///   - match: All (``MonarchConditionMatch/all``) or any (``MonarchConditionMatch/any``).
    ///     The switch shows when this is given and there are two or more conditions.
    ///   - maxConditions: The most conditions; a note replaces the add button at this count.
    ///   - showsErrors: Shows what is missing under each incomplete condition, for example
    ///     after a save attempt.
    ///   - defaultField: The id of the field new conditions start on. By default the first field.
    ///   - today: The day a date value starts on when "Choose a date" is tapped. By default now.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeID: Makes the id of a new condition.
    public init(
        conditions: Binding<[MonarchCondition]>,
        fields: [MonarchConditionField],
        match: Binding<MonarchConditionMatch>? = nil,
        maxConditions: Int = 20,
        showsErrors: Bool = false,
        defaultField: String? = nil,
        today: @escaping () -> Date = { Date() },
        labels: MonarchConditionEditorLabels = MonarchConditionEditorLabels(),
        makeID: @escaping @MainActor () -> String = { UUID().uuidString }
    ) {
        _conditions = conditions
        self.fields = fields
        self.match = match
        self.maxConditions = max(maxConditions, 0)
        self.showsErrors = showsErrors
        self.defaultField = defaultField
        self.today = today
        self.text = MonarchConditionEditorText(labels: labels)
        self.makeID = makeID
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchConditionEditorTokens.spacing) {
            if let match, MonarchConditionEditorTokens.showsMatch(hasBinding: true, count: conditions.count) {
                matchLine(match)
            }
            if conditions.isEmpty {
                muted(text.empty, style: .body)
            } else {
                VStack(alignment: .leading, spacing: MonarchConditionEditorTokens.spacing) {
                    ForEach(Array(conditions.enumerated()), id: \.element.id) { index, condition in
                        MonarchConditionRow(
                            rule: binding(condition.id, fallback: condition),
                            fields: fields,
                            text: text.row(index + 1),
                            problem: MonarchQueryBuilderTokens.shownProblem(
                                condition,
                                fields: fields,
                                showsErrors: showsErrors
                            ),
                            defaultDate: today,
                            onRemove: { remove(condition.id, position: index + 1) }
                        )
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(text.label)
            }
            if MonarchConditionEditorTokens.canAdd(count: conditions.count, maxConditions: maxConditions) {
                Button(text.add, systemImage: MonarchConditionEditorTokens.addSymbol) {
                    conditions = MonarchConditionEditorTokens.adding(
                        to: conditions,
                        id: makeID(),
                        fields: fields,
                        defaultField: defaultField
                    )
                }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(fields.isEmpty)
            } else {
                muted(text.maxReached(maxConditions), style: .caption)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func matchLine(_ match: Binding<MonarchConditionMatch>) -> some View {
        let picker = Picker(text.matchLabel, selection: match) {
            ForEach(MonarchConditionMatch.allCases, id: \.self) { combinator in
                Text(text.match(combinator)).tag(combinator)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .fixedSize()
        .accessibilityLabel(text.matchLabel)
        .monarchMinimumHitTarget()
        let before = muted(text.matchBefore, style: .body).accessibilityHidden(true)
        let after = muted(text.matchAfter, style: .body).accessibilityHidden(true)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchConditionEditorTokens.matchSpacing) {
                before
                picker
                after
            }
            VStack(alignment: .leading, spacing: MonarchConditionEditorTokens.matchSpacing) {
                before
                picker
                after
            }
        }
    }

    private func muted(_ string: String, style: MonarchTextStyle) -> some View {
        Text(string)
            .monarchTextStyle(style)
            .foregroundStyle(
                isEnabled ? theme.colors[role: MonarchConditionEditorTokens.muted] : theme.colors.textDisabled
            )
    }

    private func remove(_ id: String, position: Int) {
        conditions.removeAll { $0.id == id }
        AccessibilityNotification.Announcement(text.removed(position)).post()
    }

    private func binding(_ id: String, fallback: MonarchCondition) -> Binding<MonarchCondition> {
        Binding(
            get: { conditions.first { $0.id == id } ?? fallback },
            set: { new in
                if let index = conditions.firstIndex(where: { $0.id == id }) { conditions[index] = new }
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchConditionEditor") {
        let fields = [
            MonarchConditionField(
                id: "priority",
                label: "Priority",
                kind: .select([
                    MonarchComboboxOption(id: "high", label: "High"),
                    MonarchComboboxOption(id: "low", label: "Low"),
                ])
            ),
            MonarchConditionField(id: "subject", label: "Subject", kind: .text, prompt: "Text"),
            MonarchConditionField(id: "hours", label: "Hours since created", kind: .number, prompt: "Hours"),
            MonarchConditionField(id: "vip", label: "VIP customer", kind: .boolean),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchConditionEditor(
                    conditions: .constant([
                        MonarchCondition(id: "c1", field: "priority", operator: .equals, value: .option("high")),
                        MonarchCondition(id: "c2", field: "hours", operator: .atLeast, value: .number(4)),
                        MonarchCondition(id: "c3", field: "vip", operator: .isTrue),
                    ]),
                    fields: fields,
                    match: .constant(.all)
                )
                MonarchConditionEditor(
                    conditions: .constant([MonarchCondition(id: "e", field: nil)]),
                    fields: fields,
                    showsErrors: true
                )
                MonarchConditionEditor(conditions: .constant([]), fields: fields)
            }
        }
    }
#endif
