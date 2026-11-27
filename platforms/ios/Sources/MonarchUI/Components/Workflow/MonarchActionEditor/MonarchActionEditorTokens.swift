import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchActionEditor``: the app's replacements, else the built-in ones.
struct MonarchActionEditorText {
    let labels: MonarchActionEditorLabels

    private func pick(_ value: String?, _ key: String) -> String { value ?? MonarchActionEditorStrings.string(key) }

    var label: String { pick(labels.label, "label") }
    var type: String { pick(labels.type, "type") }
    var add: String { pick(labels.add, "add") }
    var empty: String { pick(labels.empty, "empty") }
    var choose: String { pick(labels.choose, "choose") }
    var unavailable: String { pick(labels.unavailable, "unavailable") }
    var moveUp: String { pick(labels.moveUp, "moveUp") }
    var moveDown: String { pick(labels.moveDown, "moveDown") }

    func action(_ position: Int) -> String {
        labels.action?(position) ?? MonarchActionEditorStrings.string("action", position)
    }
    func remove(_ position: Int) -> String {
        labels.remove?(position) ?? MonarchActionEditorStrings.string("remove", position)
    }
    func required(_ label: String) -> String {
        labels.required?(label) ?? MonarchActionEditorStrings.string("required", label)
    }
    func outOfRange(_ label: String) -> String {
        labels.outOfRange?(label) ?? MonarchActionEditorStrings.string("outOfRange", label)
    }
    func unknownType(_ id: String) -> String {
        labels.unknownType?(id) ?? MonarchActionEditorStrings.string("unknownType", id)
    }
    func maxReached(_ max: Int) -> String {
        labels.maxReached?(max) ?? MonarchActionEditorStrings.string("maxReached", max)
    }
    func reorder(_ position: Int) -> String {
        labels.reorder?(position) ?? MonarchActionEditorStrings.string("reorder", position)
    }
    func moved(_ position: Int) -> String {
        labels.moved?(position) ?? MonarchActionEditorStrings.string("moved", position)
    }
    func chosen(_ count: Int) -> String { labels.chosen?(count) ?? MonarchActionEditorStrings.string("chosen", count) }
}

/// A heading of the type menu and the types under it.
struct MonarchActionEditorTypeSection {
    /// The heading; `nil` for types without a group, listed first.
    let title: String?
    let types: [MonarchActionType]
}

/// Component tokens and editing logic for ``MonarchActionEditor``.
enum MonarchActionEditorTokens {
    /// The gap between actions.
    static let listSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of an action.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between an icon and its text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The minimum height of the multi-line text input: about three lines.
    static let multilineMinHeight: CGFloat = MonarchTokens.Space.space20
    /// The symbol of the add button.
    static let addSymbol = "plus"
    /// The symbol of the remove buttons.
    static let removeSymbol = "xmark"
    /// The symbol of the reorder menus.
    static let reorderSymbol = "arrow.up.arrow.down"
    /// The symbol before a problem.
    static let problemSymbol = "exclamationmark.circle.fill"

    /// The type menu's sections: types without a group first, then each group in the order it first appears.
    static func sections(_ types: [MonarchActionType]) -> [MonarchActionEditorTypeSection] {
        var order: [String] = []
        for type in types { if let group = type.group, !order.contains(group) { order.append(group) } }
        let ungrouped = types.filter { $0.group == nil }
        let grouped = order.map { group in
            MonarchActionEditorTypeSection(title: group, types: types.filter { $0.group == group })
        }
        return (ungrouped.isEmpty ? [] : [MonarchActionEditorTypeSection(title: nil, types: ungrouped)]) + grouped
    }

    /// A type's name in the menu, with the reason when it is disabled: "Escalate (Plan limit)".
    static func menuLabel(_ type: MonarchActionType) -> String {
        guard type.isDisabled, let reason = type.disabledReason, !reason.isEmpty else { return type.label }
        return "\(type.label) (\(reason))"
    }

    /// The type a new action starts as: the preferred one if it exists, else the first one not disabled.
    static func startType(_ types: [MonarchActionType], preferred: String?) -> MonarchActionType? {
        if let preferred, let type = types.first(where: { $0.id == preferred }) { return type }
        return types.first { !$0.isDisabled }
    }

    /// The parameters shown for an action: those whose `visibleWhen` holds.
    static func visibleParameters(
        _ type: MonarchActionType,
        values: [String: MonarchActionValue]
    ) -> [MonarchActionParameter] {
        type.parameters.filter { $0.visibleWhen?(values) ?? true }
    }

    /// Whether a required parameter is missing.
    static func isMissing(_ parameter: MonarchActionParameter, values: [String: MonarchActionValue]) -> Bool {
        guard parameter.isRequired else { return false }
        return (values[parameter.key] ?? parameter.blankValue).isEmpty
    }

    /// Whether a number is outside the minimum and maximum of its parameter.
    static func isOutOfRange(_ parameter: MonarchActionParameter, values: [String: MonarchActionValue]) -> Bool {
        guard case .number(let minimum, let maximum) = parameter.kind,
            case .number(let number?) = values[parameter.key]
        else { return false }
        return number < (minimum ?? -.infinity) || number > (maximum ?? .infinity)
    }

    /// The text under a parameter: its problem, or `nil`.
    static func error(
        _ parameter: MonarchActionParameter,
        values: [String: MonarchActionValue],
        text: MonarchActionEditorText
    ) -> String? {
        if isMissing(parameter, values: values) { return text.required(parameter.label) }
        if isOutOfRange(parameter, values: values) { return text.outOfRange(parameter.label) }
        return nil
    }

    /// The type's own check, or `nil`.
    static func customProblem(_ type: MonarchActionType?, values: [String: MonarchActionValue]) -> String? {
        guard let message = type?.validate?(values), !message.isEmpty else { return nil }
        return message
    }

    /// The actions with one moved by an offset, or unchanged when it cannot move.
    static func moving(_ actions: [MonarchRuleAction], id: String, by offset: Int) -> [MonarchRuleAction] {
        guard let index = actions.firstIndex(where: { $0.id == id }) else { return actions }
        let target = index + offset
        guard actions.indices.contains(target) else { return actions }
        var next = actions
        next.swapAt(index, target)
        return next
    }

    /// The spoken name of an action: "Action 1, Set priority".
    static func accessibilityLabel(
        position: Int,
        action: MonarchRuleAction,
        type: MonarchActionType?,
        text: MonarchActionEditorText
    ) -> String {
        "\(text.action(position)), \(type?.label ?? text.unknownType(action.type))"
    }

    /// The warning on an action of a disabled type, with its reason.
    static func unavailableMessage(_ type: MonarchActionType, text: MonarchActionEditorText) -> String {
        guard let reason = type.disabledReason, !reason.isEmpty else { return text.unavailable }
        return "\(text.unavailable) \(reason)"
    }

    /// The text of a multi-select: the chosen labels when one is chosen, else the count, else Choose….
    static func multiSelectSummary(
        _ chosen: [String],
        options: [MonarchComboboxOption],
        prompt: String?,
        text: MonarchActionEditorText
    ) -> String {
        switch chosen.count {
        case 0: return prompt ?? text.choose
        case 1: return options.first { $0.id == chosen[0] }?.label ?? chosen[0]
        default: return text.chosen(chosen.count)
        }
    }

    /// A list value with one id added or removed, in the options' order.
    static func toggling(_ id: String, isOn: Bool, in chosen: [String], options: [MonarchComboboxOption]) -> [String] {
        var set = Set(chosen)
        if isOn { set.insert(id) } else { set.remove(id) }
        let known = options.map(\.id).filter(set.contains)
        let unknown = chosen.filter { set.contains($0) && !options.map(\.id).contains($0) }
        return known + unknown
    }
}
