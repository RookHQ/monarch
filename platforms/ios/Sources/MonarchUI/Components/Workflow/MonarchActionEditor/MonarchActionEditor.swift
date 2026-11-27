import Foundation
import SwiftUI

/// Edits the ordered list of actions a rule runs. Each action has a type and
/// the inputs that type defines (team, priority, tags, a message…).
///
/// ```swift
/// @State private var actions = [MonarchRuleAction(id: "a1", type: "priority", parameters: ["priority": .option("high")])]
///
/// MonarchActionEditor(actions: $actions, types: [
///     MonarchActionType(id: "priority", label: "Set priority", parameters: [
///         MonarchActionParameter(key: "priority", label: "Priority", kind: .select([
///             MonarchComboboxOption(id: "high", label: "High"),
///             MonarchComboboxOption(id: "low", label: "Low"),
///         ]), isRequired: true),
///     ]),
///     MonarchActionType(id: "note", label: "Add a note", parameters: [
///         MonarchActionParameter(key: "text", label: "Note", kind: .multilineText, isRequired: true),
///     ]),
/// ])
/// ```
///
/// Each action is a card with a type menu (`Picker` with the menu style,
/// sectioned by group) and labelled inputs built from Monarch components:
/// ``MonarchTextField``, ``MonarchNumberInput``, ``MonarchTagInput``, a menu
/// `Picker`, a `Menu` of checkmarks and a checkbox `Toggle`. Check the list
/// with `problems(of:types:)` before saving and pass `showsErrors`
/// to show what is missing.
///
/// Accessibility: the list is a container named "Actions"; each card is a
/// container named "Action 1, Set priority" with Move up and Move down as
/// accessibility actions when the list is ordered, and the move is announced.
/// Required inputs carry "Required" in their hint and their error is text with
/// an icon. Touch targets are at least 44 points.
public struct MonarchActionEditor: View {
    @Binding private var actions: [MonarchRuleAction]
    private let types: [MonarchActionType]
    private let isOrdered: Bool
    private let maxActions: Int
    private let showsErrors: Bool
    private let defaultType: String?
    private let labels: MonarchActionEditorLabels
    private let makeID: @MainActor () -> String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// An action editor.
    ///
    /// - Parameters:
    ///   - actions: The actions, in the order they run.
    ///   - types: The kinds of action available.
    ///   - isOrdered: Whether actions run in order and can be moved.
    ///   - maxActions: The most actions; the add button is replaced by a note at this count.
    ///   - showsErrors: Shows what is missing on each incomplete action, for example after a save attempt.
    ///   - defaultType: The type new actions start as. The first type that is not disabled by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeID: Makes the id of a new action.
    public init(
        actions: Binding<[MonarchRuleAction]>,
        types: [MonarchActionType],
        isOrdered: Bool = true,
        maxActions: Int = 20,
        showsErrors: Bool = false,
        defaultType: String? = nil,
        labels: MonarchActionEditorLabels = MonarchActionEditorLabels(),
        makeID: @escaping @MainActor () -> String = { UUID().uuidString }
    ) {
        _actions = actions
        self.types = types
        self.isOrdered = isOrdered
        self.maxActions = max(maxActions, 0)
        self.showsErrors = showsErrors
        self.defaultType = defaultType
        self.labels = labels
        self.makeID = makeID
    }

    /// What is wrong with an action: empty required inputs that are visible, numbers outside their
    /// range, then the type's own check. Empty when the action is complete or its type is unknown.
    nonisolated public static func problems(
        of action: MonarchRuleAction,
        types: [MonarchActionType]
    ) -> [MonarchActionProblem] {
        guard let type = types.first(where: { $0.id == action.type }) else { return [] }
        var out: [MonarchActionProblem] = []
        for parameter in MonarchActionEditorTokens.visibleParameters(type, values: action.parameters) {
            if MonarchActionEditorTokens.isMissing(parameter, values: action.parameters) {
                out.append(.missing(parameter: parameter.key))
            } else if MonarchActionEditorTokens.isOutOfRange(parameter, values: action.parameters) {
                out.append(.outOfRange(parameter: parameter.key))
            }
        }
        if let custom = MonarchActionEditorTokens.customProblem(type, values: action.parameters) {
            out.append(.invalid(message: custom))
        }
        return out
    }

    /// The problems of every incomplete action, by action id. Empty when all are complete.
    nonisolated public static func problems(
        of actions: [MonarchRuleAction],
        types: [MonarchActionType]
    ) -> [String: [MonarchActionProblem]] {
        var out: [String: [MonarchActionProblem]] = [:]
        for action in actions {
            let found = problems(of: action, types: types)
            if !found.isEmpty { out[action.id] = found }
        }
        return out
    }

    /// The text of a problem, in the built-in English or the replacements in `labels`, for an app that
    /// shows the problems outside the editor. A problem of a parameter the type does not have reads as
    /// its key.
    nonisolated public static func message(
        for problem: MonarchActionProblem,
        in type: MonarchActionType,
        labels: MonarchActionEditorLabels = MonarchActionEditorLabels()
    ) -> String {
        let text = MonarchActionEditorText(labels: labels)
        func label(_ key: String) -> String { type.parameters.first { $0.key == key }?.label ?? key }
        switch problem {
        case .missing(let key): return text.required(label(key))
        case .outOfRange(let key): return text.outOfRange(label(key))
        case .invalid(let message): return message
        }
    }

    private var text: MonarchActionEditorText { MonarchActionEditorText(labels: labels) }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchActionEditorTokens.listSpacing) {
            if actions.isEmpty {
                Text(text.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                MonarchActionEditorCard(
                    action: binding(action),
                    position: index + 1,
                    count: actions.count,
                    types: types,
                    isOrdered: isOrdered,
                    showsErrors: showsErrors,
                    text: text,
                    onMove: { move(action.id, by: $0) },
                    onRemove: { actions.removeAll { $0.id == action.id } }
                )
            }
            if actions.count < maxActions {
                Button(text.add, systemImage: MonarchActionEditorTokens.addSymbol) {
                    let start = MonarchActionEditorTokens.startType(types, preferred: defaultType)
                    actions.append(MonarchRuleAction(id: makeID(), type: start))
                }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(types.allSatisfy(\.isDisabled))
            } else {
                Text(text.maxReached(maxActions))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.label)
    }

    private func move(_ id: String, by offset: Int) {
        let next = MonarchActionEditorTokens.moving(actions, id: id, by: offset)
        guard next != actions, let position = next.firstIndex(where: { $0.id == id }) else { return }
        actions = next
        AccessibilityNotification.Announcement(text.moved(position + 1)).post()
    }

    private func binding(_ action: MonarchRuleAction) -> Binding<MonarchRuleAction> {
        Binding(
            get: { actions.first { $0.id == action.id } ?? action },
            set: { new in
                if let index = actions.firstIndex(where: { $0.id == action.id }) { actions[index] = new }
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchActionEditor") {
        let types = [
            MonarchActionType(
                id: "priority",
                label: "Set priority",
                description: "Changes the ticket's priority.",
                parameters: [
                    MonarchActionParameter(
                        key: "priority",
                        label: "Priority",
                        kind: .select([
                            MonarchComboboxOption(id: "high", label: "High"),
                            MonarchComboboxOption(id: "low", label: "Low"),
                        ]),
                        isRequired: true
                    )
                ]
            ),
            MonarchActionType(
                id: "note",
                label: "Add a note",
                group: "Messages",
                parameters: [MonarchActionParameter(key: "text", label: "Note", kind: .multilineText, isRequired: true)]
            ),
            MonarchActionType(
                id: "sms",
                label: "Send SMS",
                group: "Messages",
                isDisabled: true,
                disabledReason: "Plan limit"
            ),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchActionEditor(
                    actions: .constant([
                        MonarchRuleAction(id: "a", type: "priority", parameters: ["priority": .option("high")]),
                        MonarchRuleAction(id: "b", type: "note"),
                        MonarchRuleAction(id: "c", type: "sms"),
                    ]),
                    types: types,
                    showsErrors: true
                )
                MonarchActionEditor(actions: .constant([]), types: types).disabled(true)
            }
        }
    }
#endif
